package com.pagewall.app;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.provider.OpenableColumns;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;

public class MainActivity extends Activity {

    SharedPreferences preferences;

    LinearLayout root;
    LinearLayout libraryList;

    TextView status;
    TextView pageNumber;
    TextView bookTitle;

    ImageView imageView;

    PdfRenderer renderer;
    ParcelFileDescriptor fileDescriptor;

    File currentBook;

    int currentPage = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        preferences = getSharedPreferences(
            "PageWallData", MODE_PRIVATE
        );

        File booksFolder = new File(getFilesDir(), "books");

        if (!booksFolder.exists()) {
            booksFolder.mkdirs();
        }

        showLibrary();
    }

    private void showLibrary() {

        closePdf();

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(20, 25, 20, 20);
        root.setBackgroundColor(Color.WHITE);

        TextView title = new TextView(this);
        title.setText("PAGEWALL");
        title.setTextSize(30);
        title.setGravity(Gravity.CENTER);
        title.setTextColor(Color.BLACK);

        TextView subtitle = new TextView(this);
        subtitle.setText("YOUR PERSONAL DIGITAL LIBRARY");
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setTextSize(14);
        subtitle.setTextColor(Color.DKGRAY);

        Button upload = new Button(this);
        upload.setText("+ ADD A BOOK");

        upload.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("application/pdf");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(intent, 100);
        });

        TextView heading = new TextView(this);
        heading.setText("MY BOOKS");
        heading.setTextSize(21);
        heading.setTextColor(Color.BLACK);
        heading.setPadding(0, 20, 0, 10);

        ScrollView scrollView = new ScrollView(this);

        libraryList = new LinearLayout(this);
        libraryList.setOrientation(LinearLayout.VERTICAL);

        scrollView.addView(libraryList);

        root.addView(title);
        root.addView(subtitle);
        root.addView(upload);
        root.addView(heading);

        root.addView(
            scrollView,
            new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1
            )
        );

        setContentView(root);

        loadBooks();
    }

    private void loadBooks() {

        libraryList.removeAllViews();

        File folder = new File(getFilesDir(), "books");
        File[] books = folder.listFiles();

        if (books == null || books.length == 0) {

            TextView empty = new TextView(this);
            empty.setText(
                "Your library is empty.\nAdd your first PDF book!"
            );
            empty.setGravity(Gravity.CENTER);
            empty.setTextSize(17);
            empty.setPadding(15, 40, 15, 40);

            libraryList.addView(empty);
            return;
        }

        for (File book : books) {

            if (!book.isFile() ||
                !book.getName().endsWith(".pdf")) {
                continue;
            }

            String fileName = book.getName();

            String name = preferences.getString(
                "title_" + fileName,
                fileName
            );

            int savedPage = preferences.getInt(
                "page_" + fileName, 0
            );

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(15, 15, 15, 15);
            card.setBackgroundColor(0xFFEFEFEF);

            TextView bookName = new TextView(this);
            bookName.setText("📖 " + name);
            bookName.setTextSize(19);
            bookName.setTextColor(Color.BLACK);

            TextView progress = new TextView(this);
            progress.setText(
                "Last reading page: " + (savedPage + 1)
            );
            progress.setTextSize(14);

            Button open = new Button(this);
            open.setText("OPEN BOOK");

            open.setOnClickListener(v -> {
                try {
                    openBook(book);
                } catch (Exception e) {
                    statusMessage(
                        "Unable to open book: " + e.getMessage()
                    );
                }
            });

            card.addView(bookName);
            card.addView(progress);
            card.addView(open);

            LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                );

            params.setMargins(0, 8, 0, 12);

            libraryList.addView(card, params);
        }
    }

    private void statusMessage(String message) {
        android.widget.Toast.makeText(
            this,
            message,
            android.widget.Toast.LENGTH_LONG
        ).show();
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
                addBook(data.getData());
            } catch (Exception e) {
                statusMessage("Upload failed: " + e.getMessage());
            }
        }
    }

    private void addBook(Uri uri) throws IOException {

        String originalName = "My Book.pdf";

        Cursor cursor = getContentResolver().query(
            uri,
            null,
            null,
            null,
            null
        );

        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(
                        OpenableColumns.DISPLAY_NAME
                    );

                    if (index >= 0) {
                        originalName = cursor.getString(index);
                    }
                }
            } finally {
                cursor.close();
            }
        }

        String safeName = originalName.replaceAll(
            "[^a-zA-Z0-9._-]", "_"
        );

        if (!safeName.toLowerCase().endsWith(".pdf")) {
            safeName += ".pdf";
        }

        String fileName =
            System.currentTimeMillis() + "_" + safeName;

        File folder = new File(getFilesDir(), "books");
        File destination = new File(folder, fileName);

        try (
            InputStream input =
                getContentResolver().openInputStream(uri);
            FileOutputStream output =
                new FileOutputStream(destination)
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

        preferences.edit()
            .putString("title_" + fileName, originalName)
            .putInt("page_" + fileName, 0)
            .apply();

        showLibrary();

        statusMessage("Book added to your library!");
    }

    private void openBook(File book) throws IOException {

        closePdf();

        currentBook = book;

        fileDescriptor = ParcelFileDescriptor.open(
            book,
            ParcelFileDescriptor.MODE_READ_ONLY
        );

        renderer = new PdfRenderer(fileDescriptor);

        currentPage = preferences.getInt(
            "page_" + book.getName(), 0
        );

        if (currentPage >= renderer.getPageCount()) {
            currentPage = 0;
        }

        showReader();
        displayPage();
    }

    private void showReader() {

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(12, 15, 12, 12);
        root.setBackgroundColor(Color.WHITE);

        Button back = new Button(this);
        back.setText("← MY LIBRARY");

        back.setOnClickListener(v -> showLibrary());

        bookTitle = new TextView(this);
        bookTitle.setText(
            preferences.getString(
                "title_" + currentBook.getName(),
                "My Book"
            )
        );
        bookTitle.setTextSize(20);
        bookTitle.setTextColor(Color.BLACK);
        bookTitle.setGravity(Gravity.CENTER);

        status = new TextView(this);
        status.setGravity(Gravity.CENTER);

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

        root.addView(back);
        root.addView(bookTitle);
        root.addView(status);
        root.addView(imageView, imageParams);
        root.addView(pageNumber);
        root.addView(controls);

        setContentView(root);
    }

    private void displayPage() {

        if (renderer == null) return;

        PdfRenderer.Page page = renderer.openPage(currentPage);

        try {

            int screenWidth =
                getResources().getDisplayMetrics().widthPixels - 40;

            float scale =
                Math.min(1f, (float) screenWidth / page.getWidth());

            int width = Math.max(
                1, (int) (page.getWidth() * scale)
            );

            int height = Math.max(
                1, (int) (page.getHeight() * scale)
            );

            Bitmap bitmap = Bitmap.createBitmap(
                width,
                height,
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

            preferences.edit()
                .putInt(
                    "page_" + currentBook.getName(),
                    currentPage
                )
                .apply();

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
    protected void onPause() {
        super.onPause();

        if (currentBook != null) {
            preferences.edit()
                .putInt(
                    "page_" + currentBook.getName(),
                    currentPage
                )
                .apply();
        }
    }

    @Override
    protected void onDestroy() {
        closePdf();
        super.onDestroy();
    }
}
