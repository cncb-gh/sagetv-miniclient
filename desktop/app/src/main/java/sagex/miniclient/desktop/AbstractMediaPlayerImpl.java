package sagex.miniclient.desktop;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import sagex.miniclient.MiniClient;
import sagex.miniclient.MiniPlayerPlugin;
import sagex.miniclient.events.VideoInfoShow;
import sagex.miniclient.video.VideoInfoResponse;
import sagex.miniclient.net.HasPushBuffer;
import sagex.miniclient.net.PushBufferDataSource;
import sagex.miniclient.prefs.PrefStore;
import sagex.miniclient.uibridge.Dimension;
import sagex.miniclient.uibridge.Rectangle;
import sagex.miniclient.uibridge.RectangleF;
import sagex.miniclient.util.AspectModeManager;
import sagex.miniclient.util.VerboseLogging;
import sagex.miniclient.util.VideoInfo;
import sagex.miniclient.video.HasVideoInfo;

//import org.videolan.libvlc.LibVLC;

/**
 * Created by seans on 06/10/15.
 */
public abstract class AbstractMediaPlayerImpl<Player, DataSource> implements MiniPlayerPlugin, HasVideoInfo {
    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    protected static final long PTS_ROLLOVER = 0x200000000L * 1000000L / 90000L / 1000L;
    protected static final long TWENTY_HOURS = 20 * 60 * 60 * 1000;


    protected final MiniClient client;

    protected boolean pushMode;
    protected boolean playerReady;

    protected Player player;
    protected DataSource dataSource;

    protected boolean createPlayerOnUI = true;
    protected boolean waitForPlayer = true;

    protected int state;
    protected boolean eos=false;

    protected String lastUri;

    protected long lastMediaTime = 0;

    protected boolean flushed = false;

    protected VideoInfo videoInfo = null;

    // this is mainly for testing... when we force a different UI aspect than what we really have
    protected boolean uiAspectChanged=true;
    protected Rectangle lastVidSrc=null, lastVidDest=null;

    protected AspectModeManager aspectModeManager = new AspectModeManager();

    protected boolean debug_ar=false;

    protected String failedMessage=null;

    public AbstractMediaPlayerImpl(MiniClient client, boolean createPlayerOnUI, boolean waitForPlayer, String failedMsg) {
        this.client=client;
        //this.mSurface = activity.getVideoView();
        this.createPlayerOnUI = createPlayerOnUI;
        this.waitForPlayer = waitForPlayer;
        state = NO_STATE;
        videoInfo = new VideoInfo();
        debug_ar = client.properties().getBoolean(PrefStore.Keys.debug_ar, false);
        this.failedMessage=failedMsg;
    }

    public Player getPlayer() {
        return player;
    }

    @Override
    public void free() {
        if (VerboseLogging.DETAILED_PLAYER_LOGGING) log.info("Freeing Media Player");
        releasePlayer();
    }

    @Override
    public void setPushMode(boolean b) {
        this.pushMode = b;
    }

    @Override
    public void load(byte majorHint, byte minorHint, String encodingHint, final String urlString, String hostname, boolean timeshifted, long buffersize) {
        lastUri = urlString;
        lastMediaTime = 0;
        eos=false;
        log.debug("load(): url: {}", urlString);
        if (createPlayerOnUI) {
            runOnUiThread(() -> {
                releasePlayer();
                state = LOADED_STATE;

                setupVideoFrame();

                setupPlayer(urlString);
                if (dataSource == null)
                    throw new RuntimeException("setupPlayer must create a datasource");
            });
        } else {
            releasePlayer();
            state = LOADED_STATE;
            setupPlayer(urlString);
            if (dataSource == null)
                throw new RuntimeException("setupPlayer must create a datasource");
        }

        if (debug_ar) {
            // if debug AR is turned on then show the AR UI immediately
            client.eventbus().post(new VideoInfoShow());
        }
    }

    protected void playerFailed() {
        stop();
        state = EOS_STATE;
        eos=true;
        releasePlayer();
        notifySageTVStop();
        message(String.format(failedMessage, lastUri));
    }

    protected void notifySageTVStop() {
        // This causes queded up items to fail to play.. so we can't really do this.
        // EventRouter.post(MiniclientApplication.get().getClient(), EventRouter.MEDIA_STOP);
    }

    @Override
    public long getMediaTimeMillis(long lastServerTime) {
        if (!playerReady) {
            if (VerboseLogging.DETAILED_PLAYER_LOGGING)
                log.debug("getMediaTimeMillis(): Player not ready, returning 0");
            return 0;
        }
        if (state == STOPPED_STATE) {
            if (VerboseLogging.DETAILED_PLAYER_LOGGING)
                log.debug("getMediaTimeMillis(): Player State Stopped");
            return 0;
        }

        if (state == EOS_STATE || state == NO_STATE || state == LOADED_STATE) {
            if (VerboseLogging.DETAILED_PLAYER_LOGGING)
                log.debug("getMediaTimeMillis(): Player State Not Ready {} returning last time {}", state, lastMediaTime);
            return lastMediaTime;
        }
        // when paused just keep using the last known time
        if (state == PAUSE_STATE) {
            if (VerboseLogging.DETAILED_PLAYER_LOGGING)
                log.debug("getMediaTimeMillis(): Player is paused returning last time: {}", lastMediaTime);
            return lastMediaTime;
        }
        long mt = getPlayerMediaTimeMillis(lastServerTime);
        if (flushed && mt <= 0) {
            if (VerboseLogging.DETAILED_PLAYER_LOGGING) {
                log.debug("getMediaTimeMillis() is {} after a flush/seek.  Using 0, until data shows up.", mt);
            }
            return 0;
        }
        if (mt <= 0) return 0;
        // we have some data, so we are not flushing/seeking
        flushed = false;
        lastMediaTime = mt;
        return mt;
    }

    @Override
    public int getState() {
        if (eos) return MiniPlayerPlugin.EOS_STATE;
        return state;
    }

    @Override
    public void setMute(boolean b) {

    }

    @Override
    public void stop() {
        state = STOPPED_STATE;
        removeVideoFrame();
    }

    protected void clearSurface() {
//        if (mSurface != null) {
//            context.runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    try {
//                        log.debug("Clearing Canvas");
//                        Canvas canvas = mSurface.getHolder().lockCanvas(null);
//                        canvas.drawColor(Color.BLACK);
//                        mSurface.getHolder().unlockCanvasAndPost(canvas);
//                    } catch (Throwable t) {
//                        log.debug("Failed to clear canvas");
//                    }
//                }
//            });
//        }
    }

    @Override
    public void pause() {
        state = PAUSE_STATE;
        if (VerboseLogging.DETAILED_PLAYER_LOGGING) log.debug("pause()");
        //if (createPlayerOnUI) waitForPlayer();
    }

    @Override
    public void play() {
        state = PLAY_STATE;
        if (VerboseLogging.DETAILED_PLAYER_LOGGING) log.debug("play()");
        //waitForPlayer();
    }

    @Override
    public void seek(long timeInMS) {
        if (VerboseLogging.DETAILED_PLAYER_LOGGING) log.debug("SEEK: {}", timeInMS);
    }

    @Override
    public void setServerEOS() {
        // tell the datasource that we have all the data
        if (dataSource != null && dataSource instanceof HasPushBuffer) {
            ((HasPushBuffer) dataSource).setEOS();
        }
        // we don't set our EOS until AFTER the player stream has ended
        //eos=true;
        //state=EOS_STATE;
        log.debug("Server sent us EOS");
    }

    @Override
    public long getLastFileReadPos() {
        if (dataSource instanceof HasPushBuffer) {
            return ((HasPushBuffer) dataSource).getBytesRead();
        } else {
            // return (long)player.getPosition();
            return 0;
        }
    }

    @Override
    public int getVolume() {
        return 0;
    }

    @Override
    public int setVolume(float v) {
        return 0;
    }

    @Override
    public void setVideoRectangles(final Rectangle srcRect, final Rectangle destRect, boolean hideCursor) {
        if (VerboseLogging.DETAILED_PLAYER_LOGGING)
            log.debug("setVideoRectangles: SRC: {}, DEST: {}", srcRect, destRect);
        if (srcRect==null||destRect==null) return;

        if (debug_ar) {
            lastVidSrc = srcRect.copy();
            lastVidDest = destRect.copy();
        }

        // we are using our our aspect modes
        if (srcRect.width==0) {
            // need to translate the destRect from 0,0,4096,4096 where x,y is center not bottom right
            Rectangle rect = destRect.copy();
            rect.x = destRect.x - (destRect.width/2);
            rect.y = destRect.y - (destRect.height/2);
            Dimension screen = client.getUIRenderer().getMaxScreenSize();
            rect.x = (int)(screen.getWidth() * ((float)rect.x/4096f));
            rect.y = (int)(screen.getHeight() * ((float)rect.y/4096f));
            rect.width = (int)(screen.getWidth() * ((float)destRect.width/4096f));
            rect.height = (int)(screen.getHeight() * ((float)destRect.height/4096f));

            if (uiAspectChanged || videoInfo.changed || !videoInfo.destRect.equals(rect)) {
                if (rect.x==0) {
                    Rectangle arRect = aspectModeManager.doMeasure(videoInfo, rect.asFloatRect(), client.getUIRenderer().getUIAspectRatio()).asIntRect();
                    videoInfo.updateDestRect(arRect.asFloatRect());
                    if (uiAspectChanged || videoInfo.changed) {
                        videoInfo.changed=false;
                        updatePlayerView(arRect);
                        log.debug("Updating Full Screen Video View from {} to {} adjusted with AR: {}", destRect, rect, arRect);
                    }
                } else {
                    RectangleF vid = aspectModeManager.doMeasure(videoInfo, rect.asFloatRect().position(0,0),client.getUIRenderer().getUIAspectRatio());
                    log.debug("Updating Window Video View Video in View {}", vid);
                    // adust video for the dest rect offset
                    vid.x = vid.x + rect.x;
                    vid.y = vid.y + rect.y;
                    videoInfo.updateDestRect(vid);
                    if (uiAspectChanged || videoInfo.changed) {
                        videoInfo.changed=false;
                        updatePlayerView(vid.asIntRect());
                        log.debug("Updating Window Video View from {} to {} with video: {}", destRect, rect, vid);
                    }
                }
                uiAspectChanged=false;
            }
            return;
        }

        // aspect modes coming from sagetv server if we get here
        if (uiAspectChanged || videoInfo.changed || !videoInfo.size.equals(srcRect) || !videoInfo.destRect.equals(destRect)) {
            uiAspectChanged=false;
            // need to adjust video size/position
            videoInfo.size.update(srcRect);
            videoInfo.destRect.update(destRect);
            videoInfo.changed=false;
            updatePlayerView(videoInfo.destRect.asIntRect());
        }
    }

    @Override
    public Dimension getVideoDimensions() {
        return videoInfo.size.getDimension().asIntDimension();
    }

    @Override
    public void pushData(byte[] cmddata, int bufDataOffset, int buffSize) throws IOException {
        //log.debug("pushData()");
        if (dataSource instanceof HasPushBuffer) {
            ((HasPushBuffer) dataSource).pushBytes(cmddata, bufDataOffset, buffSize);
        }
    }

    @Override
    public void flush() {
        if (VerboseLogging.DETAILED_PLAYER_LOGGING) log.debug("dispose()");
        if (dataSource instanceof HasPushBuffer) {
            ((HasPushBuffer) dataSource).flush();
        }
        flushed = true;
    }

    @Override
    public int getBufferLeft() {
        if (dataSource instanceof HasPushBuffer) {
            if (state == EOS_STATE || eos) return -1;
            return ((HasPushBuffer) dataSource).bufferAvailable();
        } else {
            return PushBufferDataSource.PIPE_SIZE;
        }
    }

    @Override
    public void run() {

    }

    protected void setVideoSize(int width, int height, float ar) {
        videoInfo.update(width, height, ar);
        updatePlayerView();
    }

    protected void setVideoSize(int width, int height, int sarNum, int sarDen) {
        videoInfo.update(width,height,sarNum, sarDen);
        updatePlayerView();
    }

    protected void releasePlayer() {
        log.debug("Releasing Player");
        player = null;
        videoInfo.reset();
        releaseDataSource();
        dataSource = null;
        state = EOS_STATE;
        eos=true;
        uiAspectChanged=true;
        removeVideoFrame();
    }

    protected void releaseDataSource() {
        if (dataSource instanceof HasPushBuffer) {
            ((HasPushBuffer) dataSource).release();
        }
    }

    @Override
    public void setVideoAdvancedAspect(String aspectMode) {
        this.videoInfo.updateARMode(aspectMode);
        updatePlayerView();
    }

    public void updatePlayerView() {
        Dimension screen = client.getUIRenderer().getMaxScreenSize();
        Rectangle rect = aspectModeManager.doMeasure(videoInfo, new RectangleF(0,0,screen.width, screen.height), client.getUIRenderer().getUIAspectRatio()).asIntRect();

        if (VerboseLogging.DETAILED_PLAYER_LOGGING)
            log.debug("updatePlayerView: Video Size {}, Screen Size {}, Calculated: {}", videoInfo, videoInfo.destRect, rect);

        updatePlayerView(rect);
    }

    @Override
    public VideoInfoResponse getVideoInfo() {
        if (player!=null) {
            VideoInfoResponse vi = new VideoInfoResponse();
            vi.videoInfo = videoInfo.copy();
            vi.uiAspectRatio = client.getUIRenderer().getUIAspectRatio();
            vi.uiScreenSizePixels = new Rectangle(0,0,client.getUIRenderer().getMaxScreenSize().width, client.getUIRenderer().getMaxScreenSize().height).asFloatRect();
            vi.uri = lastUri;
            vi.mediaTime = lastMediaTime;
            vi.state=state;
            vi.pushMode=pushMode;
            return vi;
        }
        return null;
    }

    public void notifyUIAspectChanged() {
        uiAspectChanged=true;
        if (debug_ar) {
            log.debug("UI Aspect Ratio Changed:  Notifying Video.");
            // only do this if we are debugging AR, since that's really when this would happen
            setVideoRectangles(lastVidSrc, lastVidDest, true);
        }
    }

    protected abstract void runOnUiThread(Runnable commands);

    protected abstract void setupVideoFrame();

    protected abstract void removeVideoFrame();

    protected abstract void setupPlayer(String sageTVurl);

    public abstract void message(final String msg);

    public abstract void updatePlayerView(final Rectangle rect);

    /**
     * Delegate the media time to the actual player implementation.  lastServerTime is passed
     * so that if the player needs to adjust the time based on the last time the buffer had dispose
     * then it can use this value.
     *
     * @param lastServerTime
     * @return
     */
    protected abstract long getPlayerMediaTimeMillis(long lastServerTime);

}
