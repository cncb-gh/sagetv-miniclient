package sagex.miniclient.android.video;

import android.net.Uri;
import android.widget.Toast;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;

import sagex.miniclient.android.gdx.MiniClientGDXActivity;
import sagex.miniclient.android.video.vlc.VLCInstance;
import sagex.miniclient.android.video.vlc.VLCOptions;
import sagex.miniclient.httpbridge.DataSource;
import sagex.miniclient.httpbridge.PushBufferDataSource;

//import org.videolan.libvlc.LibVLC;

/**
 * Created by seans on 06/10/15.
 */
public class VLCMediaPlayerImpl extends DataSourceMediaPlayerImpl<MediaPlayer> {
    public VLCMediaPlayerImpl(MiniClientGDXActivity activity) {
        super(activity, true);
    }

    @Override
    public long getMediaTimeMillis() {
        return player.getTime();
    }

    @Override
    public void stop() {
        player.stop();
    }

    @Override
    public void pause() {
        super.pause();
        if (player.isPlaying()) {
            player.pause();
        }
    }

    @Override
    public void seek(long maxValue) {
        log.debug("SEEK: {}", maxValue);
        if (player != null) {
            player.setPosition(maxValue);
        }
    }

    @Override
    public void play() {
        super.play();
        if (!player.isPlaying()) {
            log.warn("Calling MediaPlayer.play()");
            player.play();
        } else {
            log.warn("Player was already playing");
        }
    }

    @Override
    public long getLastFileReadPos() {
        DataSource dataSource = getDataSource();
        if (dataSource instanceof PushBufferDataSource) {
            return ((PushBufferDataSource) dataSource).getBytesRead();
        } else {
            return (long) player.getPosition();
        }
    }

    @Override
    public void flush() {
        super.flush();
        player.setPosition(Long.MAX_VALUE);
        getDataSource().flush();
    }

    protected void setupPlayer(String url) {
        log.debug("Creating Player");
        try {
            // Create a new media player
            LibVLC.setOnNativeCrashListener(new LibVLC.OnNativeCrashListener() {
                @Override
                public void onNativeCrash() {
                    log.error("**** LIBVLC CRASHED ****");
                }
            });

            log.debug("Creating VLC");
            // Setup VLC
            Media media = new Media(VLCInstance.get(context), Uri.parse(url));
            //media = new Media(VLCInstance.get(context), Uri.parse("http://192.168.1.176:8000/twd1.mp4"));
            // Media media = new Media(VLCInstance.get(context), "/sdcard/Movies/small.ts");
            VLCOptions.setMediaOptions(media, context, VLCOptions.MEDIA_VIDEO);
            log.debug("Sending URL to libbvc: {}", media.getUri());

            //libvlc.playMRL("http://techslides.com/demos/sample-videos/small.mp4");
            //libvlc.playMRL("http://ia801903.us.archive.org/18/items/rmE163ArabicSubHdarabRunnersTeamBingutopHangukSib.mkv/rmE163ArabicSubHdarabRunnersTeamBingutopHangukSib.mp4");
            log.debug("LibVLC has our URL: {} ({})", url, media);

            player = new MediaPlayer(media);

            log.debug("Have A SURFCE HOLDER? {}", mSurface.getHolder() != null);

            //player.getVLCVout().setVideoSurface(mSurface.getHolder().getSurface(), mSurface.getHolder());
            player.getVLCVout().setVideoView(mSurface);
            player.getVLCVout().attachViews();

            playerReady = true;
        } catch (Exception e) {
            log.error("Error Creating Player", e);
            Toast.makeText(context, "Error creating player!", Toast.LENGTH_LONG).show();
        }
    }

    protected void releasePlayer() {
        if (player != null) {
            try {
                if (player.isPlaying()) {
                    player.stop();
                }
                player.release();
            } catch (Throwable t) {
            }
        }
        try {
            VLCInstance.release(context);
        } catch (Throwable t) {
        }

        super.releasePlayer();
    }
}
