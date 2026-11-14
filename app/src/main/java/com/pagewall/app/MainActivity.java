
package com.pagewall.app;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;

public class MainActivity extends Activity {

    TextView status;
    TextView pageNumber;
    ImageView imageView;

    PdfRenderer renderer;
    ParcelFileDescriptor fileDescriptor;

    int currentPage = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(20, 25, 20, 20);
        layout.setBackgroundColor(Color.WHITE);

        TextView title = new TextView(this);
        title.setText("PAGEWALL");
        title.setTextSize(28);
        title.setGravity(Gravity.CENTER);
        title.setTextColor(Color.BLACK);

        status = new TextView(this);
        status.setText("Your personal digital library.");
        status.setGravity(Gravity.CENTER);
        status.setTextSize(16);

        Button upload = new Button(this);
        upload.setText("UPLOAD YOUR BOOK");

        imageView = new ImageView(this);
        imageView.setAdjustViewBounds(true);
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);

        LinearLayout.LayoutParams imageParams =
            new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1
            );

        pageNumber = new TextView(this);
        pageNumber.setGravity(Gravity.CENTER);
        pageNumber.setTextColor(Color.BLACK);

        LinearLayout controls = new LinearLayout(this);
        controls.setGravity(Gravity.CENTER);

        Button previous = new Button(this);
        previous.setText("PREVIOUS");

        Button next = new Button(this);
        next.setText("NEXT");

        upload.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("application/pdf");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(intent, 100);
        });

        previous.setOnClickListener(v -> {
            if (currentPage > 0) {
                currentPage--;
                displayPage();
            }
        });

        next.setOnClickListener(v -> {
            if (renderer != null &&
                currentPage < renderer.getPageCount() - 1) {
                currentPage++;
                displayPage();
            }
        });

        controls.addView(previous);
        controls.addView(next);

        layout.addView(title);
        layout.addView(status);
        layout.addView(upload);
        layout.addView(imageView, imageParams);
        layout.addView(pageNumber);
        layout.addView(controls);

        setContentView(layout);
    }

    @Override
    protected void onActivityResult(
        int requestCode,
        int resultCode,
        Intent data
    ) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 100 &&
            resultCode == RESULT_OK &&
            data != null) {

            try {
                openPdf(data.getData());
            } catch (Exception e) {
                status.setText("Unable to open PDF: " + e.getMessage());
            }
        }
    }

    private void openPdf(Uri uri) throws IOException {

        closePdf();

        File file = new File(getCacheDir(), "pagewall_book.pdf");

        try (
            InputStream input = getContentResolver().openInputStream(uri);
            FileOutputStream output = new FileOutputStream(file)
        ) {
            if (input == null) {
                throw new IOException("Cannot read selected file");
            }

            byte[] buffer = new byte[8192];
            int length;

            while ((length = input.read(buffer)) != -1) {
                output.write(buffer, 0, length);
            }
        }

        fileDescriptor = ParcelFileDescriptor.open(
            file,
            ParcelFileDescriptor.MODE_READ_ONLY
        );

        renderer = new PdfRenderer(fileDescriptor);

        currentPage = 0;

        status.setText("Your book is open!");
        displayPage();
    }

    private void displayPage() {

        if (renderer == null) return;

        PdfRenderer.Page page = renderer.openPage(currentPage);

        try {
            Bitmap bitmap = Bitmap.createBitmap(
                page.getWidth(),
                page.getHeight(),
                Bitmap.Config.ARGB_8888
            );

            page.render(
                bitmap,
                null,
                null,
                PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
            );

            imageView.setImageBitmap(bitmap);

            pageNumber.setText(
                "Page " + (currentPage + 1) +
                " of " + renderer.getPageCount()
            );

        } finally {
            page.close();
        }
    }

    private void closePdf() {

        if (renderer != null) {
            renderer.close();
            renderer = null;
        }

        if (fileDescriptor != null) {
            try {
                fileDescriptor.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            fileDescriptor = null;
        }
    }

    @Override
    protected void onDestroy() {
        closePdf();
        super.onDestroy();
    }
}
