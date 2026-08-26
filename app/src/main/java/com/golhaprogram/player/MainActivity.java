package com.golhaprogram.player;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.common.util.concurrent.ListenableFuture;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private final Handler main = new Handler(Looper.getMainLooper());
    private final List<Category> categories = new ArrayList<>();
    private final List<Program> programs = new ArrayList<>();
    private final List<Program> allPrograms = new ArrayList<>();

    private RecyclerView list;
    private Toolbar toolbar;
    private TextView status, emptyState, nowPlaying;
    private MaterialButton refresh;
    private TextInputEditText searchInput;
    private View searchLayout, miniPlayer;
    private ImageButton play, prev, next;

    private MediaController controller;
    private ListenableFuture<MediaController> controllerFuture;
    private Category currentCategory;
    private int currentIndex = -1;
    private boolean showingPrograms = false;

    private static final String API = "https://api.github.com/repos/golhaprogram/golhaprogram-website/contents/content/programs/";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = findViewById(R.id.toolbar);
        status = findViewById(R.id.status);
        refresh = findViewById(R.id.refreshButton);
        list = findViewById(R.id.list);
        searchLayout = findViewById(R.id.searchLayout);
        searchInput = findViewById(R.id.searchInput);
        emptyState = findViewById(R.id.emptyState);
        miniPlayer = findViewById(R.id.miniPlayer);
        nowPlaying = findViewById(R.id.nowPlaying);
        play = findViewById(R.id.playButton);
        prev = findViewById(R.id.prevButton);
        next = findViewById(R.id.nextButton);

        list.setLayoutManager(new LinearLayoutManager(this));
        refresh.setOnClickListener(v -> {
            if (showingPrograms && currentCategory != null) loadPrograms(currentCategory);
            else showCategories();
        });

        toolbar.setNavigationOnClickListener(v -> showCategories());

        play.setOnClickListener(v -> {
            if (controller != null) {
                if (controller.isPlaying()) controller.pause();
                else controller.play();
                updatePlayButton();
            }
        });
        prev.setOnClickListener(v -> playRelative(-1));
        next.setOnClickListener(v -> playRelative(1));

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filterPrograms(s.toString()); }
            @Override public void afterTextChanged(Editable s) {}
        });

        categories.add(new Category("گلهای رنگارنگ", "golhaye-rangarang", "GR"));
        categories.add(new Category("گلهای تازه", "golhaye-tazeh", "GT"));
        categories.add(new Category("گلهای جاویدان", "golhaye-javidan", "GJ"));
        categories.add(new Category("یک شاخه گل", "yek-shakhe-gol", "YSG"));
        categories.add(new Category("گلهای صحرایی", "golhaye-sahraee", "GS"));

        showCategories();
    }

    @Override
    protected void onStart() {
        super.onStart();
        SessionToken token = new SessionToken(this,
                new android.content.ComponentName(this, PlaybackService.class));
        controllerFuture = new MediaController.Builder(this, token).buildAsync();
        controllerFuture.addListener(() -> {
            try {
                controller = controllerFuture.get();
                controller.addListener(new androidx.media3.common.Player.Listener() {
                    @Override public void onIsPlayingChanged(boolean isPlaying) { updatePlayButton(); }
                    @Override public void onMediaItemTransition(MediaItem item, int reason) { updatePlayButton(); }
                });
                updatePlayButton();
            } catch (Exception ignored) {}
        }, androidx.core.content.ContextCompat.getMainExecutor(this));
    }

    @Override
    protected void onStop() {
        if (controller != null) {
            controller.release();
            controller = null;
        }
        // Releasing the UI controller does NOT stop PlaybackService.
        // MediaSessionService keeps audio alive with the screen off.
        super.onStop();
    }

    @Override
    public void onBackPressed() {
        if (showingPrograms) showCategories();
        else super.onBackPressed();
    }

    private void showCategories() {
        showingPrograms = false;
        currentCategory = null;
        toolbar.setTitle("برنامه گلها");
        toolbar.setNavigationIcon(null);
        searchLayout.setVisibility(View.GONE);
        refresh.setVisibility(View.GONE);
        emptyState.setVisibility(View.GONE);
        status.setText("یک مجموعه را انتخاب کنید");
        list.setAdapter(new CategoryAdapter(categories, this::loadPrograms));
    }

    private void loadPrograms(Category c) {
        showingPrograms = true;
        currentCategory = c;
        toolbar.setTitle(c.title);
        toolbar.setNavigationIcon(android.R.drawable.ic_media_previous);
        searchLayout.setVisibility(View.VISIBLE);
        refresh.setVisibility(View.VISIBLE);
        searchInput.setText("");
        emptyState.setVisibility(View.GONE);
        status.setText(R.string.loading);
        programs.clear();
        allPrograms.clear();
        list.setAdapter(new ProgramAdapter(programs, this::startProgram));

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                String json = get(API + c.slug);
                JSONArray arr = new JSONArray(json);
                List<Program> result = new ArrayList<>();
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);
                    String name = o.optString("name");
                    if (name.endsWith(".md") && !name.equals("_index.md")) {
                        String number = name.substring(0, name.length() - 3);
                        result.add(new Program(c.title, c.prefix, number));
                    }
                }
                result.sort((a, b) -> compareProgramNumbers(a.number, b.number));
                main.post(() -> {
                    allPrograms.clear();
                    allPrograms.addAll(result);
                    programs.clear();
                    programs.addAll(result);
                    list.setAdapter(new ProgramAdapter(programs, this::startProgram));
                    status.setText(programs.size() + " برنامه");
                    emptyState.setVisibility(programs.isEmpty() ? View.VISIBLE : View.GONE);
                    emptyState.setText(R.string.no_programs);
                });
            } catch (Exception e) {
                main.post(() -> {
                    status.setText(R.string.network_error);
                    emptyState.setVisibility(View.VISIBLE);
                    emptyState.setText("فهرست برنامه‌ها دریافت نشد.\nلطفاً اتصال اینترنت را بررسی کنید و دوباره «به‌روزرسانی» را بزنید.");
                });
            }
        });
    }

    private void filterPrograms(String query) {
        if (!showingPrograms) return;
        String q = query == null ? "" : query.trim().toLowerCase();
        programs.clear();
        if (q.isEmpty()) {
            programs.addAll(allPrograms);
        } else {
            for (Program p : allPrograms) {
                if (p.number.toLowerCase().contains(q) || p.displayName().toLowerCase().contains(q)) {
                    programs.add(p);
                }
            }
        }
        list.setAdapter(new ProgramAdapter(programs, this::startProgram));
        status.setText(programs.size() + " برنامه");
        emptyState.setVisibility(programs.isEmpty() ? View.VISIBLE : View.GONE);
        emptyState.setText("برنامه‌ای با این جستجو پیدا نشد.");
    }

    private int compareProgramNumbers(String a, String b) {
        try { return Integer.compare(Integer.parseInt(a), Integer.parseInt(b)); }
        catch (Exception ignored) { return a.compareToIgnoreCase(b); }
    }

    private void startProgram(Program p, int position) {
        currentIndex = position;
        nowPlaying.setText(p.displayName());
        miniPlayer.setVisibility(View.VISIBLE);

        if (controller == null) {
            Toast.makeText(this, "پخش‌کننده هنوز آماده نشده است", Toast.LENGTH_SHORT).show();
            return;
        }

        MediaItem item = new MediaItem.Builder()
                .setMediaId(p.prefix + "_" + p.number)
                .setUri(p.audioUrl())
                .setMediaMetadata(new MediaMetadata.Builder()
                        .setTitle(p.displayName())
                        .setArtist(p.category)
                        .build())
                .build();

        controller.setMediaItem(item);
        controller.prepare();
        controller.play();
        updatePlayButton();
    }

    private void playRelative(int delta) {
        if (currentIndex < 0 || programs.isEmpty()) return;
        int target = currentIndex + delta;
        if (target < 0) target = programs.size() - 1;
        if (target >= programs.size()) target = 0;
        startProgram(programs.get(target), target);
    }

    private void updatePlayButton() {
        if (controller != null) {
            play.setImageResource(controller.isPlaying()
                    ? android.R.drawable.ic_media_pause
                    : android.R.drawable.ic_media_play);
        }
    }

    private static String get(String address) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(address).openConnection();
        c.setConnectTimeout(15000);
        c.setReadTimeout(20000);
        c.setRequestProperty("Accept", "application/vnd.github+json");
        c.setRequestProperty("User-Agent", "GolhaPlayer/1.1");
        int code = c.getResponseCode();
        InputStream in = code >= 200 && code < 300 ? c.getInputStream() : c.getErrorStream();
        if (in == null) throw new Exception("HTTP " + code);
        try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder s = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) s.append(line);
            if (code < 200 || code >= 300) throw new Exception("HTTP " + code);
            return s.toString();
        } finally { c.disconnect(); }
    }
}
