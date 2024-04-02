package com.xncoder.advanceprotection.FaceDetection;

import android.graphics.Bitmap;
import android.graphics.RectF;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.List;

public interface SimilarityClassifier {
    void register(String name, Recognition recognition);

    List<Recognition> recognizeImage(Bitmap bitmap, boolean getExtra);

    class Recognition {
        private String id;
        private String title;
        private Float distance;
        private Object extra;
        private RectF location;
        private Integer color;
        private Bitmap crop;

        public Recognition(
                final String id, final String title, final Float distance, final RectF location) {
            this.id = id;
            this.title = title;
            this.distance = distance;
            this.location = location;
            this.color = null;
            this.extra = null;
            this.crop = null;
        }

        public void setExtra(Object extra) {
            this.extra = extra;
        }

        public Object getExtra() {
            return this.extra;
        }

        public void setColor(Integer color) {
            this.color = color;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public Float getDistance() {
            return distance;
        }

        public void setDistance(float distance) {
            this.distance = distance;
        }

        public RectF getLocation() {
            return new RectF(location);
        }

        public void setLocation(RectF location) {
            this.location = location;
        }

        @NonNull
        @Override
        public String toString() {
            String resultString = "";
            if (id != null) {
                resultString += "[" + id + "] ";
            }

            if (title != null) {
                resultString += title + " ";
            }

            if (distance != null) {
                resultString += String.format("(%.1f%%) ", distance * 100.0f);
            }

            if (location != null) {
                resultString += location + " ";
            }

            return resultString.trim();
        }

        public Integer getColor() {
            return this.color;
        }

        public void setCrop(Bitmap crop) {
            this.crop = crop;
        }

        public Bitmap getCrop() {
            return this.crop;
        }
    }
}
