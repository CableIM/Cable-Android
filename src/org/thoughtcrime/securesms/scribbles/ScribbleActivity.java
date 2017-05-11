package org.thoughtcrime.securesms.scribbles;

import android.annotation.TargetApi;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PointF;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;

import org.thoughtcrime.securesms.PassphraseRequiredActionBarActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.crypto.MasterSecret;
import org.thoughtcrime.securesms.providers.PersistentBlobProvider;
import org.thoughtcrime.securesms.scribbles.viewmodel.Font;
import org.thoughtcrime.securesms.scribbles.viewmodel.Layer;
import org.thoughtcrime.securesms.scribbles.viewmodel.TextLayer;
import org.thoughtcrime.securesms.scribbles.widget.MotionView;
import org.thoughtcrime.securesms.scribbles.widget.ScribbleView;
import org.thoughtcrime.securesms.scribbles.widget.VerticalSlideColorPicker;
import org.thoughtcrime.securesms.scribbles.widget.entity.ImageEntity;
import org.thoughtcrime.securesms.scribbles.widget.entity.MotionEntity;
import org.thoughtcrime.securesms.scribbles.widget.entity.TextEntity;
import org.thoughtcrime.securesms.util.MediaUtil;
import org.thoughtcrime.securesms.util.concurrent.ListenableFuture;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class ScribbleActivity extends PassphraseRequiredActionBarActivity implements ScribbleToolbar.ScribbleToolbarListener, VerticalSlideColorPicker.OnColorChangeListener {

  private static final String TAG = ScribbleActivity.class.getName();

  public static final int SELECT_STICKER_REQUEST_CODE = 123;
  public static final int SCRIBBLE_REQUEST_CODE       = 31424;

  private VerticalSlideColorPicker colorPicker;
  private ScribbleToolbar          toolbar;
  private ScribbleView             scribbleView;
  private MasterSecret             masterSecret;

  @Override
  protected void onCreate(Bundle savedInstanceState, @NonNull MasterSecret masterSecret) {
    setContentView(R.layout.scribble_activity);

    this.masterSecret = masterSecret;
    this.scribbleView = (ScribbleView) findViewById(R.id.scribble_view);
    this.toolbar      = (ScribbleToolbar) findViewById(R.id.toolbar);
    this.colorPicker  = (VerticalSlideColorPicker) findViewById(R.id.scribble_color_picker);

    this.toolbar.setListener(this);
    this.toolbar.setToolColor(Color.RED);

    scribbleView.setMotionViewCallback(motionViewCallback);
    scribbleView.setDrawingMode(false);
    scribbleView.setImage(getIntent().getData(), masterSecret);

    colorPicker.setOnColorChangeListener(this);
    colorPicker.setVisibility(View.GONE);

    setSupportActionBar(toolbar);

    getSupportActionBar().setDisplayHomeAsUpEnabled(false);
    getSupportActionBar().setTitle(null);
  }

  private void addSticker(final Bitmap pica) {
    scribbleView.post(new Runnable() {
      @Override
      public void run() {
        Layer       layer  = new Layer();
        ImageEntity entity = new ImageEntity(layer, pica, scribbleView.getWidth(), scribbleView.getHeight());

        scribbleView.addEntityAndPosition(entity);
      }
    });
  }

  private void changeTextEntityColor(int selectedColor) {
    TextEntity textEntity = currentTextEntity();

    if (textEntity == null) {
      return;
    }

    textEntity.getLayer().getFont().setColor(selectedColor);
    textEntity.updateEntity();
    scribbleView.invalidate();
  }

  private void startTextEntityEditing() {
    TextEntity textEntity = currentTextEntity();
    if (textEntity != null) {
      scribbleView.startEditing(textEntity);
    }
  }

  @Nullable
  private TextEntity currentTextEntity() {
    if (scribbleView != null && scribbleView.getSelectedEntity() instanceof TextEntity) {
      return ((TextEntity) scribbleView.getSelectedEntity());
    } else {
      return null;
    }
  }

  protected void addTextSticker() {
    TextLayer  textLayer  = createTextLayer();
    TextEntity textEntity = new TextEntity(textLayer, scribbleView.getWidth(), scribbleView.getHeight());
    scribbleView.addEntityAndPosition(textEntity);

    // move text sticker up so that its not hidden under keyboard
    PointF center = textEntity.absoluteCenter();
    center.y = center.y * 0.5F;
    textEntity.moveCenterTo(center);

    // redraw
    scribbleView.invalidate();

    startTextEntityEditing();
    changeTextEntityColor(toolbar.getToolColor());
  }

  private TextLayer createTextLayer() {
    TextLayer textLayer = new TextLayer();
    Font font = new Font();

    font.setColor(TextLayer.Limits.INITIAL_FONT_COLOR);
    font.setSize(TextLayer.Limits.INITIAL_FONT_SIZE);

    textLayer.setFont(font);

    return textLayer;
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (resultCode == RESULT_OK) {
      if (requestCode == SELECT_STICKER_REQUEST_CODE) {
        if (data != null) {
          toolbar.setStickerSelected(true);
          final String stickerFile = data.getStringExtra(StickerSelectActivity.EXTRA_STICKER_FILE);

          new AsyncTask<Void, Void, Bitmap>() {
            @Override
            protected @Nullable
            Bitmap doInBackground(Void... params) {
              try {
                return BitmapFactory.decodeStream(getAssets().open(stickerFile));
              } catch (IOException e) {
                Log.w(TAG, e);
                return null;
              }
            }

            @Override
            protected void onPostExecute(@Nullable Bitmap bitmap) {
              addSticker(bitmap);
            }
          }.execute();
        }
      }
    }
  }

  @Override
  public void onBrushSelected(boolean enabled) {
    scribbleView.setDrawingMode(enabled);
    colorPicker.setVisibility(enabled ? View.VISIBLE : View.GONE);
  }

  @Override
  public void onPaintUndo() {
    scribbleView.undoDrawing();
  }

  @Override
  public void onTextSelected(boolean enabled) {
    if (enabled) {
      addTextSticker();
      scribbleView.setDrawingMode(false);
      colorPicker.setVisibility(View.VISIBLE);
    } else {
      scribbleView.clearSelection();
      colorPicker.setVisibility(View.GONE);
    }
  }

  @Override
  public void onStickerSelected(boolean enabled) {
    colorPicker.setVisibility(View.GONE);

    if (!enabled) {
      scribbleView.clearSelection();
    } else {
      scribbleView.setDrawingMode(false);
      Intent intent = new Intent(this, StickerSelectActivity.class);
      startActivityForResult(intent, SELECT_STICKER_REQUEST_CODE);
    }
  }

  public void onDeleteSelected() {
    scribbleView.deleteSelected();
    colorPicker.setVisibility(View.GONE);
  }

  @Override
  public void onSave() {
    ListenableFuture<Bitmap> future = scribbleView.getRenderedImage();

    future.addListener(new ListenableFuture.Listener<Bitmap>() {
      @Override
      public void onSuccess(Bitmap result) {
        PersistentBlobProvider provider = PersistentBlobProvider.getInstance(ScribbleActivity.this);
        ByteArrayOutputStream  baos     = new ByteArrayOutputStream();
        result.compress(Bitmap.CompressFormat.JPEG, 80, baos);

        byte[] data = baos.toByteArray();
        baos   = null;
        result = null;

        Uri    uri    = provider.create(masterSecret, data, MediaUtil.IMAGE_JPEG, null);
        Intent intent = new Intent();
        intent.setData(uri);
        setResult(RESULT_OK, intent);

        finish();
      }

      @Override
      public void onFailure(ExecutionException e) {
        Log.w(TAG, e);
      }
    });
  }

  private final MotionView.MotionViewCallback motionViewCallback = new MotionView.MotionViewCallback() {
    @Override
    public void onEntitySelected(@Nullable MotionEntity entity) {
      if (entity == null) {
        toolbar.setNoneSelected();
        colorPicker.setVisibility(View.GONE);
      } else if (entity instanceof TextEntity) {
        toolbar.setTextSelected(true);
        colorPicker.setVisibility(View.VISIBLE);
      } else {
        toolbar.setStickerSelected(true);
        colorPicker.setVisibility(View.GONE);
      }
    }

    @Override
    public void onEntityDoubleTap(@NonNull MotionEntity entity) {
      startTextEntityEditing();
    }
  };

  @Override
  public void onColorChange(int color) {
    if (color == 0) color = Color.RED;

    toolbar.setToolColor(color);
    scribbleView.setDrawingBrushColor(color);

    changeTextEntityColor(color);
  }
}
