package com.learnknots.wesslnelson.Sorter.model;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.renderscript.Long2;
import android.util.Log;

import com.learnknots.wesslnelson.Sorter.R;


/**
 * Created by wesslnelson on 5/18/16.
 */
public class Sortee {

    private static final String TAG = Sortee.class.getSimpleName();

    private static final int FADE_MILLISECONDS = 3000;//R.integer.fade_milliseconds;
    private static final int FADE_STEP = 120;//R.integer.fade_step;

    // Calculate our alpha step from our fade parameters
    private static final int ALPHA_STEP = 255 / (FADE_MILLISECONDS / FADE_STEP);
    private Paint alphaPaint = new Paint();
    private int currentAlpha = 255;


    private static final int LIFE_SPAN_UNSAFE   = 5000; // # of ms sortee survives while unsafe
    private static final int LIFE_SPAN_SAFE     = 2000; // # of ms sortee survives in safe zone
    public static final int STATE_ALIVE         = 0;    // sortee is younger than LIFE_SPAN
    public static final int STATE_DEAD          = 1;    // sortee is too old
    public static final int STATE_SAFE          = 0;    // sortee is in a safe zone
    public static final int STATE_UNSAFE        = 1;    // sortee is not safe

    private Bitmap bitmap;           // the animation sequence
    private Rect sourceRect;         // the rectangle to be drawn from the animation bitmap
    private int frameNr;             // number of frames in animation
    private int currentFrame;        // the current frame
    private long frameTicker;        // the time of the last frame update
    private int framePeriod;         // milliseconds between each frame (1000/fps)
    private long timeEnteringUnsafe; // when a sortee enters the unsafezone
    private long timeEnteringSafe;   // when a sortee enters the safezone
    private int lifeState;           // whether or not a sortee is alive
    private int safeState;           // whether or not a sortee is in a safezone

    private int spriteWidth;    // the width of the sprite to calculate the cut out rectangle
    private int spriteHeight;   // the height of the sprite

    private int x; // the X coordinate
    private int y; // the y coordinate
    private boolean touched; // true if sortee is touched/picked up

    private int safeZone; // x coordinate for which anything greater than is safe

    private Explosion explosion; // if dies outside of safezone then it explodes

    public Sortee(Bitmap bitmap, int x, int y, int width, int height, int fps, int frameCount, int safeZone, long startTime) {
        this.bitmap = bitmap;
        this.x = x;
        this.y = y;
        currentFrame = 0;
        frameNr = frameCount;
        spriteWidth = bitmap.getWidth() / frameCount; // all frames are the same width
        spriteHeight = bitmap.getHeight();
        sourceRect = new Rect(0, 0, spriteWidth, spriteHeight);
        framePeriod = 1000 / fps;
        frameTicker = 0l;
        this.safeZone = safeZone;
        this.timeEnteringUnsafe = startTime;
        this.lifeState = STATE_ALIVE;
        this.safeState = STATE_UNSAFE;
        this.timeEnteringSafe = -1;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }
    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public int getX() {
        return x;
    }
    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }
    public void setY(int y) {
        this.y = y;
    }

    public boolean isTouched() {
        return touched;
    }
    public void setTouched(boolean touched) {
        this.touched = touched;
    }

    public Rect getSourceRect() {
        return sourceRect;
    }
    public void setSourceRect(Rect sourceRect) {
        this.sourceRect = sourceRect;
    }

    public int getFrameNr() {
        return frameNr;
    }
    public void setFrameNr(int frameNr) {
        this.frameNr = frameNr;
    }

    public int getCurrentFrame() {
        return currentFrame;
    }
    public void setCurrentFrame(int currentFrame) {
        this.currentFrame = currentFrame;
    }

    public int getFramePeriod() {
        return framePeriod;
    }
    public void setFramePeriod(int framePeriod) {
        this.framePeriod = framePeriod;
    }

    public int getSpriteWidth() {
        return spriteWidth;
    }
    public void setSpriteWidth(int spriteWidth) {
        this.spriteWidth = spriteWidth;
    }

    public int getSpriteHeight() {
        return spriteHeight;
    }
    public void setSpriteHeight(int spriteHeight) {
        this.spriteHeight = spriteHeight;
    }

    public int getSafeZone() {
        return safeZone;
    }
    public void setSafeZone( int safeZone) {
        this.safeZone = safeZone;
    }

    public long getTimeEnteringSafe() {
        return timeEnteringSafe;
    }
    public void setTimeEnteringSafe(long time) {
        this.timeEnteringSafe = time;
    }

    public long getTimeEnteringUnsafe() {
        return timeEnteringUnsafe;
    }
    public void setTimeEnteringUnsafe(long time) {
        this.timeEnteringUnsafe = time;
    }

    public int getLifeState() {
        return lifeState;
    }
    public void setLifeState(int state) {
        this.lifeState = state;
    }

    public int getSafeState() {
        return safeState;
    }
    public void setSafeState(int state) {
        this.safeState = state;
    }

    // helper methods -------------------------
    public boolean isAlive() {
        return this.lifeState == STATE_ALIVE;
    }
    public boolean isDead() {
        return this.lifeState == STATE_DEAD;
    }
    public boolean isSafe() {
        return this.safeState == STATE_SAFE;
    }
    public boolean isUnsafe() {
        return this.safeState == STATE_UNSAFE;
    }
    public boolean tooOldForSafe(Long gameTime) {
        return (gameTime - this.timeEnteringSafe >= LIFE_SPAN_SAFE);
    }
    public boolean tooOldForUnsafe(Long gameTime) {
        return (gameTime - this.timeEnteringUnsafe >= LIFE_SPAN_UNSAFE);
    }

    public void update(long gameTime) {
        if (isAlive()) {
            // handles animating the sprite
            if (gameTime > frameTicker + framePeriod) {
                frameTicker = gameTime;
                // increment the frame
                currentFrame++;
                if (currentFrame >= frameNr) {
                    currentFrame = 0;
                }
            }
            // define the rectangle to cut out sprite
            this.sourceRect.left = currentFrame * spriteWidth;
            this.sourceRect.right = this.sourceRect.left + spriteWidth;

            // checks if in safe zone
            if (isUnsafe()) {
                if (getX() > getSafeZone()) {
                    setTimeEnteringSafe(gameTime);
                    setSafeState(STATE_SAFE);
                }
            }  else if (isSafe()) {
                if (getX() <= getSafeZone()) {
                    setTimeEnteringUnsafe(gameTime);
                    setSafeState(STATE_UNSAFE);
                }
            }

            // checks if too old
            if (isUnsafe()) {
                if (tooOldForUnsafe(gameTime)) {
                    setLifeState(STATE_DEAD);
                }
            } else if (isSafe()) {
                if (tooOldForSafe(gameTime)) {
                    setLifeState(STATE_DEAD);
                }
            }


        } else if (isUnsafe()) {
            runExplosion();
        } else if (isSafe()) {
            fadeOut();
        }

    }

    public void draw(Canvas canvas) {
        // where to draw the sprite
        if (isAlive()) {
            Rect destRect = new Rect(getX(), getY(), getX() + spriteWidth, getY() + spriteHeight);
            canvas.drawBitmap(bitmap, sourceRect, destRect, null);

        } else if (isUnsafe() && explosion != null) { // blow up old unsafe
            explosion.setX(getX());
            explosion.setY(getY());
            explosion.draw(canvas);

        } else if (isSafe()) { // fade out old safe
            Rect destRect = new Rect(getX(), getY(), getX() + spriteWidth, getY() + spriteHeight);
            canvas.drawBitmap(bitmap, sourceRect, destRect, alphaPaint);
        }
    }

    public void handleActionDown(int eventX, int eventY) {
        if (eventX >= (x - bitmap.getWidth() / 2) && (eventX <= (x + bitmap.getWidth()/2))) {
            if (eventY >= (y - bitmap.getHeight() / 2) && (y <= (y + bitmap.getHeight() / 2))) {
                // sortee touched
                setTouched(true);
            } else {
                setTouched(false);
            }
        } else {
            setTouched(false);
        }
    }

    public void runExplosion() {
        if (explosion == null) {
            explosion = new Explosion(200, getX(), getY());
        } else {
            explosion.update();
        }
    }

    public void fadeOut() {
        if (currentAlpha >10) {
            alphaPaint.setAlpha(currentAlpha);
            currentAlpha -= ALPHA_STEP;
        }

    }


}
