package sagex.miniclient.uibridge;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import sagex.miniclient.FontHolder;
import sagex.miniclient.ImageHolder;
import sagex.miniclient.UIManager;

public class LoggingUIManager<Image, Font> implements UIManager<Image, Font> {
	private UIManager<Image, Font> delegate;

	public LoggingUIManager(UIManager<Image, Font> delegate) {
		this.delegate=delegate;
	}

	Map<String,Boolean> logged = new HashMap<String, Boolean>();

	void log(String s) {
//		if (!logged.containsKey(s)) {
//			logged.put(s,true);
			System.out.println("UIManager: " + s);
//		}
	}


	public void init() {
		log("init");
		delegate.init();
	}

	public void dispose() {
		log("dispose");
		delegate.dispose();
	}

	public void close() {
		log("close");
		delegate.close();
	}

	public void refresh() {
		log("refresh");
		delegate.refresh();
	}

	public void hideCursor() {
		log("hideCursor");		
		delegate.hideCursor();
	}

	public void showBusyCursor() {
		log("showBusyCursor");		
		delegate.showBusyCursor();
	}

	public void drawRect(int x, int y, int width, int height, int thickness, int argbTL, int argbTR, int argbBR, int argbBL) {
		log("drawRect");
		delegate.drawRect(x, y, width, height, thickness, argbTL, argbTR, argbBR, argbBL);
	}

	public void fillRect(int x, int y, int width, int height, int argbTL, int argbTR, int argbBR, int argbBL) {
		log("fillRect");
		delegate.fillRect(x, y, width, height, argbTL, argbTR, argbBR, argbBL);
	}

	public void clearRect(int x, int y, int width, int height, int argbTL, int argbTR, int argbBR, int argbBL) {
		log("clearRect");
		delegate.clearRect(x, y, width, height, argbTL, argbTR, argbBR, argbBL);
	}

	public void drawOval(int x, int y, int width, int height, int thickness, int argbTL, int argbTR, int argbBR, int argbBL,
			int clipX, int clipY, int clipW, int clipH) {
		log("drawOval");
		delegate.drawOval(x, y, width, height, thickness, argbTL, argbTR, argbBR, argbBL, clipX, clipY, clipW, clipH);
	}

	public void fillOval(int x, int y, int width, int height, int argbTL, int argbTR, int argbBR, int argbBL, int clipX, int clipY,
			int clipW, int clipH) {
		log("fillOval");
		delegate.fillOval(x, y, width, height, argbTL, argbTR, argbBR, argbBL, clipX, clipY, clipW, clipH);
	}

	public void drawRoundRect(int x, int y, int width, int height, int thickness, int arcRadius, int argbTL, int argbTR, int argbBR,
			int argbBL, int clipX, int clipY, int clipW, int clipH) {
		log("drawRoundRect");
		delegate.drawRoundRect(x, y, width, height, thickness, arcRadius, argbTL, argbTR, argbBR, argbBL, clipX, clipY, clipW,
				clipH);
	}

	public void fillRoundRect(int x, int y, int width, int height, int arcRadius, int argbTL, int argbTR, int argbBR, int argbBL,
			int clipX, int clipY, int clipW, int clipH) {
		log("fillRoundRect");
		delegate.fillRoundRect(x, y, width, height, arcRadius, argbTL, argbTR, argbBR, argbBL, clipX, clipY, clipW, clipH);
	}

	public void drawTexture(int x, int y, int width, int height, int handle, ImageHolder<?> img, int srcx, int srcy, int srcwidth,
			int srcheight, int blend) {
		//log(String.format("drawTexture[%s](%s,%s,%s,%s,%s,%s,%s,%s)", handle, x,y, width, height, srcx, srcy, srcwidth,srcheight));
		delegate.drawTexture(x, y, width, height, handle, img, srcx, srcy, srcwidth, srcheight, blend);
	}

	public void drawLine(int x1, int y1, int x2, int y2, int argb1, int argb2) {
		log("drawLine");
		delegate.drawLine(x1, y1, x2, y2, argb1, argb2);
	}

	public ImageHolder<Image> loadImage(int width, int height) {
		//log(String.format("loadImage(%s,%s)",width,height));
		return delegate.loadImage(width, height);
	}

	public ImageHolder<Image> createSurface(int width, int height) {
		log(String.format("createSurface(%s,%s)", width,height));
		return delegate.createSurface(width, height);
	}

	public ImageHolder<Image> readImage(File cachedFile) throws Exception {
		log("readImage File");
		return delegate.readImage(cachedFile);
	}

	public ImageHolder<Image> readImage(ByteArrayInputStream bais) throws Exception {
		log("readImage Stream");
		return delegate.readImage(bais);
	}

	public ImageHolder<Image> newImage(int destWidth, int destHeight) {
		log(String.format("newImage(%s,%s)",destWidth, destHeight));
		return delegate.newImage(destWidth, destHeight);
	}

	public void setTargetSurface(int handle, ImageHolder<?> image) {
		log(String.format("setTargetSurface[%s]", handle));
		delegate.setTargetSurface(handle, image);
	}

	public FontHolder<Font> createFont(InputStream fis) {
		log("createFont");
		return delegate.createFont(fis);
	}

	public FontHolder<Font> loadFont(String string, int style, int size) {
		log("loadFont");
		return delegate.loadFont(string, style, size);
	}

	public FontHolder<Font> deriveFont(FontHolder<?> cachedFont, float size) {
		log("deriveFont");
		return delegate.deriveFont(cachedFont, size);
	}

	public void flipBuffer() {
		log("flipBuffer");
		delegate.flipBuffer();
	}

	public void startFrame() {
		log("startFrame");
		delegate.startFrame();
	}

	public void loadImageLine(int handle, ImageHolder<?> image, int line, int len2, byte[] cmddata) {
		//log(String.format("loadImageLine[%s](%s)", handle, line));
		delegate.loadImageLine(handle, image, line, len2, cmddata);
	}

	public void xfmImage(int srcHandle, ImageHolder<?> srcImg, int destHandle, ImageHolder<?> destImg, int destWidth,
			int destHeight, int maskCornerArc) {
		log("xfmImage");
		delegate.xfmImage(srcHandle, srcImg, destHandle, destImg, destWidth, destHeight, maskCornerArc);
	}

	public boolean hasGraphicsCanvas() {
		//log("hasGraphicsCanvas");
		return delegate.hasGraphicsCanvas();
	}

	public void drawText(int x, int y, int textlen, String text, int fontHandle, FontHolder<?> fontHolder, int argb, int clipX,
			int clipY, int clipW, int clipH) {
		log("drawText");
		delegate.drawText(x, y, textlen, text, fontHandle, fontHolder, argb, clipX, clipY, clipW, clipH);
	}

	public Dimension getMaxScreenSize() {
		log("getMaxScreenSize");
		return delegate.getMaxScreenSize();
	}

	public Dimension getScreenSize() {
		log("getScreenSize");
		return delegate.getScreenSize();
	}

	public void setFullScreen(boolean b) {
		log("setFullScreen");
		delegate.setFullScreen(b);
	}

	public void setSize(int w, int h) {
		log("setSize");
		delegate.setSize(w, h);
	}

	public void invokeLater(Runnable runnable) {
		log("invokeLater");
		delegate.invokeLater(runnable);
	}

}
