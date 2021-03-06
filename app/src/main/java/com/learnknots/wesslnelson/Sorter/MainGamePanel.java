package com.learnknots.wesslnelson.Sorter;

/**
 * Created by wesslnelson on 5/18/16.
 *
 * following tutorial by javacodegeeks.com
 */

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.learnknots.wesslnelson.Sorter.model.Sortee;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

public class MainGamePanel extends SurfaceView implements SurfaceHolder.Callback {

    Resources res = getResources();

    private static final String TAG = MainGamePanel.class.getSimpleName();
    private final int SAFE_ZONE_WIDTH = res.getInteger(R.integer.safeZone);
    private final int NEW_SORTEE_TIME = res.getInteger(R.integer.timeBetweenRespawn);
    public static final int LEFT = 0;
    public static final int RIGHT = 1;

    private long newSorteeTicker = 0;
    private String safeZoneTest;
    private String numberOfSortees;
    private int nextRespawn;
    private boolean mGameIsRunning;

    private MainThread thread;
    private List<Sortee> sortees;
    private int safeSide;
    public static int score;

    private boolean isCarrying;


    public MainGamePanel(Context context) {
        super(context);
        // adding the callback (this) to the surface holder to intercept events
        getHolder().addCallback(this);


        sortees = new ArrayList<Sortee>();
        numberOfSortees = Integer.toString(sortees.size());

        // create the main game loop thread
        //thread = new MainThread(getHolder(), this);

        // make the GamePanel focusable so it can handle events
        setFocusable(true);

        safeZoneTest = "no one is in it";
        
        isCarrying = false;
        score = 0;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // create the main game loop thread
        thread = new MainThread(getHolder(), this);

        thread.setRunning(true);
        thread.start();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(TAG, "Surface is being destroyed");
        boolean retry = true;
        while (retry) {
            try {
                thread.join();
                retry = false;
            } catch (InterruptedException e) {
                // try again shutting down the thread®
            }
        }
        Log.d(TAG, "Thread was shut down cleanly");
        Log.d(TAG, "Returning to home screen");
    }

    public void endIt() {
        thread.setRunning(false);
        ((Activity) getContext()).finish();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            // delegating event handling to the droid
            //sortee.handleActionDown((int)event.getX(), (int)event.getY());
            for (Sortee sortee : sortees) {
                if (!isCarrying) {
                    sortee.handleActionDown((int) event.getX(), (int) event.getY());
                    if (sortee.isTouched()) {
                        isCarrying = true;
                    }
                }
            }

            // check if in lower part of screen to see if exit
            if (event.getX() > getWidth() - 50 && event.getY() < 50) {
                endIt();
            } else {
                Log.d(TAG, "Coords: x=" + event.getX() + ",y=" + event.getY());
            }
        }
        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            // the gestures
            for (Sortee sortee : sortees) {
                if (sortee.isTouched()) {
                    // the droid was picked up and is being dragged
                    sortee.setX((int) event.getX());
                    sortee.setY((int) event.getY());
                }
            }
        }
        if (event.getAction() == MotionEvent.ACTION_UP) {
            // touch was released
            for (Sortee sortee : sortees) {
                if (sortee.isTouched()) {
                    sortee.setTouched(false);
                    isCarrying = false;
                }
            }
        }
        return true;
    }


    protected void render(Canvas canvas) {

        // fills the canvas with black
        canvas.drawColor(Color.BLACK);
        Rect sourceRect = new Rect(0,0,50,50);
        Rect destRect = new Rect(getWidth()-50, 0, getWidth(), 50);
        canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.exit_smaller), sourceRect, destRect, null);

        drawSafeLine(canvas, this.getWidth() - SAFE_ZONE_WIDTH);
        drawSafeLine(canvas, SAFE_ZONE_WIDTH);
        // draw all sortees in list
        for (Sortee sortee : sortees) {
            sortee.draw(canvas);
        }
        displayText(canvas, safeZoneTest, 20);
        displayText(canvas, Integer.toString(score), 60);


    }

    private void drawSafeLine(Canvas canvas, int safeZone) {
        if (canvas != null) {
            Paint paint = new Paint();
            paint.setARGB(255, 255, 255, 255);

            canvas.drawLine(safeZone, 0, safeZone, canvas.getHeight(), paint);
        }
    }


    private void displayText(Canvas canvas, String text, int yHeight) {
        if (canvas != null && text != null) {
            Paint paint = new Paint();
            paint.setARGB(255, 255, 255, 255);
            canvas.drawText(text, this.getWidth() - 150, yHeight, paint);
        }
    }


    /**
     * This is the game update method. It iterates through all the objects
     * and calls their update method if they have one or calls specific
     * engine's update method.
     */
    public void update() {

        // will eventually check if sortee has been unsorted for too long
        List<Sortee> toRemove = new ArrayList<Sortee>();
        for (Sortee sortee : sortees) {
            sortee.update(System.currentTimeMillis());
            if (sortee.isSafe()) {
                safeZoneTest = "A sortee has been sorted";
            }
            if (sortee.isDead()) {
                if (sortee.isTouched()) {
                    isCarrying = false;
                }
                if (sortee.isSafe()) {
                    score += 1;
                }
                toRemove.add(sortee);
            }
        }
        sortees.removeAll(toRemove);
        numberOfSortees = Integer.toString(sortees.size());

        randomNewSortee(System.currentTimeMillis());

    }


    public void randomNewSortee(Long time) {
        nextRespawn = NEW_SORTEE_TIME - rndInt(0, 1500);
        if (time > newSorteeTicker + nextRespawn) {
            newSorteeTicker = time;
            newSortee();

        }
    }

    // Return an integer that ranges from min inclusive to max inclusive.
    static int rndInt(int min, int max) {
        return (int) (min + Math.random() * (max - min + 1));
    }

    public void newSortee() {
        safeSide = rndInt(0,1);
        if ( safeSide == LEFT) {
            sortees.add(new Sortee(BitmapFactory.decodeResource(getResources(), R.drawable.moniter2),
                    rndInt(SAFE_ZONE_WIDTH + 25, this.getRight() - SAFE_ZONE_WIDTH - 50), rndInt(0, 400),  // initial position
                    32, 32,  // width and height of sprite
                    5, 2,    // FPS and number of frames in the animation
                    SAFE_ZONE_WIDTH, this.getRight() - SAFE_ZONE_WIDTH, // Where the left and right sort zones are
                    System.currentTimeMillis(), safeSide));  // when sortee created and which sort zone is the safe


        } else if ( safeSide == RIGHT) {
            sortees.add(new Sortee(BitmapFactory.decodeResource(getResources(), R.drawable.moniter),
                    rndInt(SAFE_ZONE_WIDTH + 25, this.getRight() - SAFE_ZONE_WIDTH - 50), rndInt(0, 400),  // initial position
                    32, 32,  // width and height of sprite
                    5, 3,    // FPS and number of frames in the animation
                    SAFE_ZONE_WIDTH, this.getRight() - SAFE_ZONE_WIDTH, // Where the left and right sort zones are
                    System.currentTimeMillis(), safeSide));  // when sortee created and which sort zone is the safe
        }


    }

}

