package com.libq.audiocropview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

/**
 * describ:音频裁剪
 * author:libq
 * data:2018
 */
public class AudioCropView extends View {

    private OnCropAreaChangedListener cropAreaChangedListener;
    private int trackWidth = 0;//track 宽

    private Paint slipPaint = null;
    private Paint maskPaint = null;
    private Paint mTrackPaint = null;
    private Paint borderPaint = null;
    private Paint thumbPaint = null;

    private int borderWidth = 5;

    private Bitmap shape = null;
    private Bitmap mask = null;

    private boolean isNewMask = true;
    private int mBackgroundColor = Color.LTGRAY;
    private int mForegroundColor = Color.BLUE;
    private int mSpaceSize = 6;
    private int mTrackItemWidth = 6;

    private int mCropLength;//裁剪区间长度 px
    private int mCropDuration = 2000;//裁剪时长 ms
    private int mAudioDuration = 2000;//音频时长 ms
    private int mCropStartPosition;//裁剪开始位置
    private int mCropEndPosition;//裁剪结束位置
    private int leftSpace = 30;//音轨左边空白，拖动条的宽度为leftSpace 的两倍


    private int mThumbHeight=28;//拖动条的高
    private int mThumbWidth;//拖动条的宽
    private int mThumbX;
    private int mThumbY;
    private int trigonHeight=6;//拖动条三角形的高度 px
    private int mThumbMarginTop =5;
    private RectF thumbRectF;
    private Path trigonPath;

    private float[] mTrackTemplateData = {0.80f,0.70f,0.40f,0.60f,0.40f,0.30f,0.50f,0.70f,0.65f,0.90f};//track中一个片段中每个竖条的高度比例

    public AudioCropView(Context context){
        super(context);
        init();
    }

    public AudioCropView(Context paramContext, AttributeSet paramAttributeSet) {
        super(paramContext, paramAttributeSet);
        init();
    }

    private void init(){
        setLongClickable(true);
        slipPaint = new Paint();
        slipPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        slipPaint.setFilterBitmap(false);

        maskPaint = new Paint();
        maskPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        maskPaint.setFilterBitmap(false);

        mTrackPaint = new Paint();
        mTrackPaint.setAntiAlias(true);
        mTrackPaint.setStrokeWidth(mTrackItemWidth);
        mTrackPaint.setColor(Color.LTGRAY);
        mTrackPaint.setStyle(Paint.Style.FILL);
        mTrackPaint.setStrokeCap(Paint.Cap.ROUND);

        borderPaint = new Paint();
        borderPaint.setAntiAlias(true);
        borderPaint.setStrokeWidth(borderWidth);
        borderPaint.setColor(Color.BLACK);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeCap(Paint.Cap.SQUARE);

        thumbPaint = new Paint();
        thumbPaint.setAntiAlias(true);
        thumbPaint.setStrokeWidth(borderWidth);
        thumbPaint.setColor(mForegroundColor);
        thumbPaint.setStyle(Paint.Style.FILL);
        thumbPaint.setStrokeCap(Paint.Cap.SQUARE);
        trigonPath = new Path();


    }



    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        int width = 0;
        int height = 0;
        //获得宽度MODE
        int modeW = MeasureSpec.getMode(widthMeasureSpec);
        //获得宽度的值
        if (modeW == MeasureSpec.AT_MOST) {
            width = MeasureSpec.getSize(widthMeasureSpec);
        }
        if (modeW == MeasureSpec.EXACTLY) {
            width = widthMeasureSpec;
        }
        if (modeW == MeasureSpec.UNSPECIFIED) {
            width = MeasureSpec.getSize(widthMeasureSpec);
        }
        //获得高度MODE
        int modeH = MeasureSpec.getMode(height);
        //获得高度的值
        if (modeH == MeasureSpec.AT_MOST) {
            height = MeasureSpec.getSize(heightMeasureSpec);
        }
        if (modeH == MeasureSpec.EXACTLY) {
            height = heightMeasureSpec;
        }
        if (modeH == MeasureSpec.UNSPECIFIED) {
            //ScrollView和HorizontalScrollView
            height = MeasureSpec.getSize(heightMeasureSpec);
        }
        mThumbY = height-mThumbHeight;
        mThumbWidth = 2*(leftSpace - mTrackItemWidth/2);
        thumbRectF = new RectF(mThumbX,mThumbY+trigonHeight,mThumbX+mThumbWidth,mThumbY+mThumbHeight);
        mCropStartPosition = leftSpace - mTrackItemWidth/2;

        //设置宽度和高度
        setMeasuredDimension(width, height);

    }

    @Override
    protected void onDraw(Canvas canvas) {
        try {

            drawThumb(canvas);
            //画背景
            drawTrack(canvas,mBackgroundColor);
            int layer = canvas.saveLayer(0, 0, getWidth(), getHeight()-mThumbHeight, null, Canvas.ALL_SAVE_FLAG);
            //画前景
            drawTrack(canvas,mForegroundColor);
            //切割
            if (shape == null || shape.isRecycled()) {
                shape = getShape(getWidth(), getHeight()-mThumbHeight);
            }
            canvas.drawBitmap(shape, 0, 0, slipPaint);

            //画透明格子
            if (isNewMask) {
                mask = getMask(getWidth(), getHeight()-mThumbHeight);
                isNewMask = false;
            }
            canvas.drawBitmap(mask, 0, 0, maskPaint);
            canvas.restoreToCount(layer);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Bitmap getShape(int width, int height) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        RectF localRectF = new RectF(0, 0, width, height);
        Paint paint = new Paint();
        paint.setAntiAlias(true); //去锯齿
        canvas.drawRect(localRectF,paint);

        return bitmap;
    }

    private Bitmap getMask(int width, int height) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setAntiAlias(true); //去锯齿
        paint.setColor(mForegroundColor);
        //paint.setAlpha(160);

        canvas.drawRect(mCropStartPosition, 0, mCropEndPosition, height, paint);

        return bitmap;
    }

    public void setAudioDuration(int duration){
        mAudioDuration = duration;
    }


    /**
     * 更新裁剪区间位置
     */
    public void updateCropArea() {
        isNewMask = true;
        this.invalidate();
    }

    /**
     * 设置裁剪时长 ms
     * @param duration
     */
    public void setAudioCropDuration(int duration){
        mCropDuration = duration;
    }

    /**
     * 获取裁剪区间长度 px
     */
    private int getCropAreaLength(){
       float percent =  (mCropDuration*1f)/(mAudioDuration * 1f);
       return mCropLength = (int)(getWidth()*percent);
    }





    //轨道-----------------------------------------------------------------------------
    private void drawTrack(Canvas canvas, int color) {

        mTrackPaint.setColor(color);
        int cy = (canvas.getHeight()-mThumbHeight) / 2 ;
        int count = (int)((getWidth() - mSpaceSize)*1f/((mTrackItemWidth+mSpaceSize)*1f));

        for (int i = 0; i<=count; i++) {
            int x =  leftSpace +(mTrackItemWidth+mSpaceSize )* i;
            //绘制的个数可能大于给定的 TrackTemplateDate 里面提供的数据，
            // 如果大于，则从头取TrackTemplateDate里面的值
            int yIndex = i%(mTrackTemplateData.length-1);
            canvas.drawLine(x, cy - mTrackTemplateData[yIndex]*(getHeight()-mThumbHeight)/2, x, cy + mTrackTemplateData[yIndex]*(getHeight()-mThumbHeight)/2, mTrackPaint);
        }

    }


    private void drawThumb(Canvas canvas){
        //切换画笔属性
        thumbPaint.setStrokeWidth(borderWidth);
        thumbPaint.setColor(mForegroundColor);
        thumbPaint.setStyle(Paint.Style.FILL);
        thumbPaint.setStrokeCap(Paint.Cap.SQUARE);
        //画拖动条顶端三角形
        trigonPath.reset();
        trigonPath.moveTo(mThumbX+leftSpace-mTrackItemWidth/2,getHeight()-mThumbHeight  );
        trigonPath.lineTo(mThumbX+leftSpace-mTrackItemWidth/2 - trigonHeight*1f*3/4,getHeight()-mThumbHeight+trigonHeight );
        trigonPath.lineTo(mThumbX+leftSpace-mTrackItemWidth/2 +trigonHeight*1f*3/4,getHeight()-mThumbHeight+trigonHeight );
        trigonPath.close();
        canvas.drawPath(trigonPath,thumbPaint);
        //画拖动条圆角矩形
        thumbRectF.left = mThumbX;
        thumbRectF.right = mThumbX + mThumbWidth;
        canvas.drawRoundRect(thumbRectF,6,6,thumbPaint);
        //切换画笔属性
        thumbPaint.setStrokeWidth(2);
        thumbPaint.setColor(Color.WHITE);
        thumbPaint.setStyle(Paint.Style.FILL);
        thumbPaint.setStrokeCap(Paint.Cap.ROUND);
        //画3条竖线
        int centerX = mThumbX + mThumbWidth/2;
        int leftX = centerX - 8;
        int rightX = centerX + 8;
        int centerY = mThumbY + trigonHeight + 8;
        int centerY2 = mThumbY + mThumbHeight - 8;

        canvas.drawLine(centerX,centerY,centerX,centerY2,thumbPaint);
        canvas.drawLine(leftX,centerY,leftX,centerY2,thumbPaint);
        canvas.drawLine(rightX,centerY,rightX,centerY2,thumbPaint);

        mCropLength = getCropAreaLength();
        mCropStartPosition = mThumbX+mThumbWidth/2;
        mCropEndPosition = mCropStartPosition + mCropLength;

        Log.e("xxxx","mCropStartPosition="+mCropStartPosition+" ,mCropEndPosition="+mCropEndPosition);

    }

    public void setBackgroundColorInt(int backgroundColor) {
        this.mBackgroundColor = backgroundColor;
        invalidate();
    }

    public void setForegroundColor(int foregroundColor) {
        this.mForegroundColor = foregroundColor;
        invalidate();
    }

    public void setSpaceSize(int spaceSize) {
        this.mSpaceSize = spaceSize;
        invalidate();
    }

    public void setTrackItemWidth(int trackItemWidth) {
        this.mTrackItemWidth = trackItemWidth;
        invalidate();
    }


    public void setTrackTemplateData(float[] mTrackTemplateData) {
        this.mTrackTemplateData = mTrackTemplateData;
        invalidate();
    }

    /**
     * 控件左边的空白,下方拖动块的宽度为，这个值得2倍
     * @param value
     */
    public void setLeftSpace(int value){
        this.leftSpace = value;
    }

    boolean isThumbCanMove = false;
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float downX = event.getX();
        float downY = event.getY();

        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                if(downX>=mThumbX&& downX<=mThumbX+mThumbWidth&&downY>=mThumbY&&downY<=mThumbY+mThumbHeight){
                    isThumbCanMove = true;
                }else{
                    isThumbCanMove = false;
                }
                break;
            case MotionEvent.ACTION_UP:
                isThumbCanMove = false;
                break;
            case MotionEvent.ACTION_MOVE:

                if(isThumbCanMove){
                    mThumbX = (int)(downX-mThumbWidth/2);
                    if(mThumbX<0){
                        mThumbX = 0;
                    }
                    if(mThumbX+mThumbWidth/2+mCropLength>getWidth()){
                        mThumbX = getWidth() - mCropLength - mThumbWidth/2;
                    }
                    mCropStartPosition = mThumbX+mThumbWidth/2;
                    thumbRectF.left = mThumbX;
                    thumbRectF.right = thumbRectF.left + mThumbWidth;
                    updateCropArea();
                    if(cropAreaChangedListener!=null){
                        float percent = mCropStartPosition * 1f/(getWidth() - leftSpace + mTrackItemWidth*1f/1f*2);
                        int startTime = (int)(mAudioDuration * percent);
                        cropAreaChangedListener.onChange(startTime,startTime+mCropDuration);
                    }
                }

                break;
        }
        return super.onTouchEvent(event);
    }

    public void setOnCropAreaChangedListener(OnCropAreaChangedListener listener){
        cropAreaChangedListener = listener;
    }

    public void reset(){
        mThumbX = 0;
        updateCropArea();
    }

    public interface OnCropAreaChangedListener{
        void onChange(int startTime,int endTime);
    }


}

