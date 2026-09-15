package com.faculdade.wifibrutelab;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.NumberFormat;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Simulador offline de busca exaustiva. Não enumera redes e não tenta conexão.
 */
public final class MainActivity extends Activity {
    private static final int BACKGROUND = Color.rgb(246, 248, 251);
    private static final int SURFACE = Color.WHITE;
    private static final int DARK = Color.rgb(22, 31, 45);
    private static final int MUTED = Color.rgb(88, 101, 119);
    private static final int BLUE = Color.rgb(38, 104, 190);
    private static final int BLUE_LIGHT = Color.rgb(231, 241, 252);
    private static final int BORDER = Color.rgb(218, 225, 234);
    private static final int GREEN = Color.rgb(28, 126, 82);
    private static final int RED = Color.rgb(179, 55, 55);
    private static final long MAX_CANDIDATES = 3000000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private volatile boolean running;
    private volatile boolean stopRequested;
    private volatile long attempted;
    private volatile long startNanos;
    private long totalCandidates;

    private EditText networkField;
    private EditText targetField;
    private EditText alphabetField;
    private EditText maxLengthField;
    private Button startButton;
    private Button stopButton;
    private Button resetButton;
    private ProgressBar progressBar;
    private TextView statusText;
    private TextView progressText;
    private TextView attemptsText;
    private TextView timeText;
    private TextView rateText;
    private TextView spaceText;
    private TextView resultText;

    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            if (!running) return;
            renderStats(attempted, System.nanoTime() - startNanos, totalCandidates);
            handler.postDelayed(this, 100L);
        }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        Window window = getWindow();
        window.setStatusBarColor(BACKGROUND);
        window.setNavigationBarColor(BACKGROUND);
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
        buildScreen();
    }

    @Override protected void onDestroy() {
        stopRequested = true;
        running = false;
        handler.removeCallbacks(ticker);
        super.onDestroy();
    }

    private void buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(BACKGROUND);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(22), dp(32), dp(22), dp(30));
        scroll.addView(content, new ScrollView.LayoutParams(-1, -2));

        TextView eyebrow = text("ALGORITMOS · EXPERIMENTO OFFLINE", 12, BLUE);
        eyebrow.setTypeface(Typeface.DEFAULT_BOLD);
        eyebrow.setLetterSpacing(.10f);
        content.addView(eyebrow);

        TextView title = text("Wi-Fi Brute Lab", 31, DARK);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        content.addView(title, params(-1, -2, 6));

        TextView intro = text("Uma bancada didática para observar a busca exaustiva, medir o custo"
                + " de cada tentativa e discutir complexidade sem acessar nenhuma rede real.",
                16, MUTED);
        intro.setLineSpacing(dp(3), 1f);
        content.addView(intro, params(-1, -2, 18));

        LinearLayout safety = card(BLUE_LIGHT);
        TextView safetyTitle = text("Modo seguro", 18, BLUE);
        safetyTitle.setTypeface(Typeface.DEFAULT_BOLD);
        safety.addView(safetyTitle);
        TextView safetyBody = text("A rede abaixo é apenas um rótulo de laboratório. Este APK não "
                + "possui permissões de Wi-Fi ou Internet, não lista SSIDs, não envia tentativas "
                + "e não salva a senha-alvo.", 14, DARK);
        safetyBody.setLineSpacing(dp(2), 1f);
        safety.addView(safetyBody, params(-1, -2, 5));
        content.addView(safety, params(-1, -2, 18));

        LinearLayout setup = card(SURFACE);
        TextView setupTitle = text("1. Configure o experimento", 19, DARK);
        setupTitle.setTypeface(Typeface.DEFAULT_BOLD);
        setup.addView(setupTitle);

        networkField = field("Identificador da rede fictícia", "LAB-WIFI-01",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        setup.addView(networkField, params(-1, dp(52), 10));

        targetField = field("Senha-alvo (usada somente na memória)", "cab3",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        setup.addView(targetField, params(-1, dp(52), 10));

        alphabetField = field("Alfabeto de busca", "abc123", InputType.TYPE_CLASS_TEXT);
        setup.addView(alphabetField, params(-1, dp(52), 10));

        maxLengthField = field("Comprimento máximo", "4", InputType.TYPE_CLASS_NUMBER);
        setup.addView(maxLengthField, params(-1, dp(52), 4));

        TextView hint = text("Dica: comece com um alfabeto pequeno e comprimento 3 ou 4. "
                + "O limite do experimento é de 3 milhões de candidatos.", 12, MUTED);
        hint.setLineSpacing(dp(2), 1f);
        setup.addView(hint, params(-1, -2, 8));

        startButton = actionButton("Iniciar simulação", true);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { startSimulation(); }
        });
        setup.addView(startButton, params(-1, dp(50), 5));

        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        stopButton = actionButton("Parar", false);
        stopButton.setEnabled(false);
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { requestStop(); }
        });
        controls.addView(stopButton, new LinearLayout.LayoutParams(0, dp(48), 1f));
        resetButton = actionButton("Limpar métricas", false);
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { resetMetrics(); }
        });
        LinearLayout.LayoutParams resetParams = new LinearLayout.LayoutParams(0, dp(48), 1f);
        resetParams.leftMargin = dp(8);
        controls.addView(resetButton, resetParams);
        setup.addView(controls, params(-1, dp(48), 2));
        content.addView(setup, params(-1, -2, 20));

        TextView statsTitle = text("2. Telemetria do algoritmo", 22, DARK);
        statsTitle.setTypeface(Typeface.DEFAULT_BOLD);
        content.addView(statsTitle, params(-1, -2, 10));

        LinearLayout stats = card(SURFACE);
        statusText = text("Pronto para iniciar", 17, BLUE);
        statusText.setTypeface(Typeface.DEFAULT_BOLD);
        stats.addView(statusText);
        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(1000);
        progressBar.setProgress(0);
        stats.addView(progressBar, params(-1, dp(10), 12));
        progressText = text("0,0%", 12, MUTED);
        stats.addView(progressText, params(-1, -2, 5));

        attemptsText = statLine(stats, "Tentativas", "0");
        timeText = statLine(stats, "Tempo decorrido", "0 ms");
        rateText = statLine(stats, "Taxa média", "0 tentativas/s");
        spaceText = statLine(stats, "Espaço de busca", "—");
        content.addView(stats, params(-1, -2, 18));

        LinearLayout result = card(SURFACE);
        TextView resultTitle = text("Resultado", 19, DARK);
        resultTitle.setTypeface(Typeface.DEFAULT_BOLD);
        result.addView(resultTitle);
        resultText = text("A senha-alvo não sai deste aparelho e é apagada ao limpar as métricas.",
                14, MUTED);
        resultText.setLineSpacing(dp(2), 1f);
        result.addView(resultText, params(-1, -2, 4));
        content.addView(result, params(-1, -2, 18));

        LinearLayout theory = card(Color.rgb(254, 249, 235));
        TextView theoryTitle = text("Para o relatório", 18, DARK);
        theoryTitle.setTypeface(Typeface.DEFAULT_BOLD);
        theory.addView(theoryTitle);
        TextView theoryBody = text("A busca percorre strings por comprimento: primeiro todas as de "
                + "1 caractere, depois as de 2 e assim por diante. Se o alfabeto tem b símbolos "
                + "e o limite é m, o pior caso testa b + b² + … + bᵐ candidatos, ou O(bᵐ). "
                + "Compare a taxa e o tempo ao aumentar b ou m.", 14, DARK);
        theoryBody.setLineSpacing(dp(2), 1f);
        theory.addView(theoryBody, params(-1, -2, 4));
        content.addView(theory, params(-1, -2, 10));

        TextView footer = text("Simulação local · sem conexão · sem armazenamento da senha", 12,
                MUTED);
        footer.setGravity(Gravity.CENTER);
        content.addView(footer, params(-1, -2, 14));

        setContentView(scroll);
    }

    private void startSimulation() {
        if (running) return;

        String network = networkField.getText().toString().trim();
        final String target = targetField.getText().toString();
        String rawAlphabet = alphabetField.getText().toString();
        String maxLengthText = maxLengthField.getText().toString().trim();

        if (network.length() == 0 || target.length() == 0 || rawAlphabet.length() == 0) {
            toast("Preencha o identificador, a senha-alvo e o alfabeto.");
            return;
        }

        final char[] alphabet = uniqueCharacters(rawAlphabet);
        if (alphabet.length == 0) {
            toast("O alfabeto precisa ter ao menos um símbolo.");
            return;
        }
        if (alphabet.length > 64) {
            toast("Use no máximo 64 símbolos no alfabeto.");
            return;
        }

        final int maxLength;
        try {
            maxLength = Integer.parseInt(maxLengthText);
        } catch (NumberFormatException error) {
            toast("O comprimento máximo precisa ser um número.");
            return;
        }
        if (maxLength < 1 || maxLength > 8) {
            toast("Escolha um comprimento entre 1 e 8.");
            return;
        }

        final long searchSpace = countCandidates(alphabet.length, maxLength);
        if (searchSpace < 0 || searchSpace > MAX_CANDIDATES) {
            toast("Espaço grande demais. Reduza o alfabeto ou o comprimento.");
            return;
        }

        totalCandidates = searchSpace;
        attempted = 0;
        stopRequested = false;
        running = true;
        startNanos = System.nanoTime();
        networkField.setEnabled(false);
        targetField.setEnabled(false);
        alphabetField.setEnabled(false);
        maxLengthField.setEnabled(false);
        startButton.setEnabled(false);
        stopButton.setEnabled(true);
        resetButton.setEnabled(false);
        statusText.setText("Executando em " + network + " (fictícia)…");
        statusText.setTextColor(BLUE);
        resultText.setText("Enumerando candidatos localmente…");
        resultText.setTextColor(MUTED);
        spaceText.setText(formatNumber(searchSpace) + " candidatos");
        renderStats(0, 0, searchSpace);
        handler.removeCallbacks(ticker);
        handler.post(ticker);

        Thread worker = new Thread(new Runnable() {
            @Override public void run() {
                String found = null;
                for (int length = 1; length <= maxLength && !stopRequested; length++) {
                    found = searchLength(alphabet, new char[length], 0, target);
                    if (found != null) break;
                }

                final String foundResult = found;
                final long finalAttempts = attempted;
                final long elapsed = System.nanoTime() - startNanos;
                final boolean wasStopped = stopRequested && foundResult == null
                        && finalAttempts < searchSpace;
                runOnUiThread(new Runnable() {
                    @Override public void run() {
                        finishSimulation(foundResult, finalAttempts, elapsed, searchSpace,
                                wasStopped);
                    }
                });
            }
        }, "offline-brute-force");
        worker.start();
    }

    private String searchLength(char[] alphabet, char[] candidate, int position, String target) {
        if (stopRequested) return null;
        if (position == candidate.length) {
            attempted++;
            if (target.equals(new String(candidate))) return new String(candidate);
            return null;
        }
        for (int i = 0; i < alphabet.length; i++) {
            candidate[position] = alphabet[i];
            String found = searchLength(alphabet, candidate, position + 1, target);
            if (found != null || stopRequested) return found;
        }
        return null;
    }

    private void requestStop() {
        if (!running) return;
        stopRequested = true;
        running = false;
        handler.removeCallbacks(ticker);
        stopButton.setEnabled(false);
        statusText.setText("Interrupção solicitada…");
        statusText.setTextColor(RED);
    }

    private void finishSimulation(String found, long finalAttempts, long elapsed,
                                  long searchSpace, boolean wasStopped) {
        running = false;
        handler.removeCallbacks(ticker);
        renderStats(finalAttempts, elapsed, searchSpace);
        networkField.setEnabled(true);
        targetField.setEnabled(true);
        alphabetField.setEnabled(true);
        maxLengthField.setEnabled(true);
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
        resetButton.setEnabled(true);

        if (found != null) {
            statusText.setText("Candidato encontrado");
            statusText.setTextColor(GREEN);
            resultText.setText("A senha-alvo foi encontrada após " + formatNumber(finalAttempts)
                    + " tentativas. Resultado: “" + found + "”.\nRede: apenas "
                    + networkField.getText().toString().trim() + " (rótulo fictício).");
            resultText.setTextColor(GREEN);
        } else if (wasStopped) {
            statusText.setText("Interrompida");
            statusText.setTextColor(RED);
            resultText.setText("A execução foi parada após " + formatNumber(finalAttempts)
                    + " tentativas; nenhum resultado foi concluído.");
            resultText.setTextColor(RED);
        } else {
            statusText.setText("Busca concluída");
            statusText.setTextColor(MUTED);
            resultText.setText("Nenhum candidato do espaço configurado corresponde à senha-alvo."
                    + " Aumente o comprimento ou inclua os símbolos corretos para outro teste.");
            resultText.setTextColor(MUTED);
        }
    }

    private void resetMetrics() {
        if (running) return;
        attempted = 0;
        totalCandidates = 0;
        statusText.setText("Pronto para iniciar");
        statusText.setTextColor(BLUE);
        progressBar.setProgress(0);
        progressText.setText("0,0%");
        attemptsText.setText("0");
        timeText.setText("0 ms");
        rateText.setText("0 tentativas/s");
        spaceText.setText("—");
        resultText.setText("A senha-alvo não sai deste aparelho e é apagada ao limpar as métricas.");
        resultText.setTextColor(MUTED);
    }

    private void renderStats(long attempts, long elapsedNanos, long searchSpace) {
        long elapsedMillis = elapsedNanos / 1000000L;
        double seconds = elapsedNanos / 1000000000.0;
        double rate = seconds > 0.0 ? attempts / seconds : 0.0;
        int progress = searchSpace > 0
                ? (int) Math.min(1000L, (attempts * 1000L) / searchSpace) : 0;
        progressBar.setProgress(progress);
        progressText.setText(String.format(Locale.US, "%.1f%%", searchSpace > 0
                ? (attempts * 100.0) / searchSpace : 0.0));
        attemptsText.setText(formatNumber(attempts));
        timeText.setText(formatDuration(elapsedMillis));
        rateText.setText(formatNumber((long) rate) + " tentativas/s");
    }

    private char[] uniqueCharacters(String input) {
        Set<Character> set = new LinkedHashSet<Character>();
        for (int i = 0; i < input.length(); i++) set.add(input.charAt(i));
        char[] result = new char[set.size()];
        int index = 0;
        for (Character character : set) result[index++] = character.charValue();
        return result;
    }

    private long countCandidates(int alphabetSize, int maxLength) {
        long power = 1L;
        long total = 0L;
        for (int length = 1; length <= maxLength; length++) {
            if (power > MAX_CANDIDATES / alphabetSize) return MAX_CANDIDATES + 1L;
            power *= alphabetSize;
            if (total > MAX_CANDIDATES - power) return MAX_CANDIDATES + 1L;
            total += power;
        }
        return total;
    }

    private EditText field(String hint, String value, int inputType) {
        EditText field = new EditText(this);
        field.setHint(hint);
        field.setText(value);
        field.setTextSize(15);
        field.setTextColor(DARK);
        field.setHintTextColor(MUTED);
        field.setSingleLine(true);
        field.setPadding(dp(14), 0, dp(14), 0);
        field.setInputType(inputType);
        field.setBackground(roundRect(SURFACE, BORDER, 10));
        return field;
    }

    private Button actionButton(String label, boolean primary) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(14);
        button.setAllCaps(false);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setTextColor(primary ? Color.WHITE : BLUE);
        button.setBackground(roundRect(primary ? BLUE : SURFACE, primary ? BLUE : BORDER, 10));
        return button;
    }

    private TextView statLine(LinearLayout parent, String label, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView labelView = text(label, 14, MUTED);
        row.addView(labelView, new LinearLayout.LayoutParams(0, dp(38), 1f));
        TextView valueView = text(value, 14, DARK);
        valueView.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        valueView.setTypeface(Typeface.DEFAULT_BOLD);
        row.addView(valueView, new LinearLayout.LayoutParams(0, dp(38), 1f));
        parent.addView(row);
        return valueView;
    }

    private LinearLayout card(int color) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(16), dp(16), dp(16), dp(16));
        layout.setBackground(roundRect(color, BORDER, 14));
        return layout;
    }

    private TextView text(String value, float size, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        return view;
    }

    private LinearLayout.LayoutParams params(int width, int height, int bottom) {
        LinearLayout.LayoutParams result = new LinearLayout.LayoutParams(width, height);
        result.bottomMargin = dp(bottom);
        return result;
    }

    private GradientDrawable roundRect(int fill, int stroke, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(radius));
        drawable.setStroke(dp(1), stroke);
        return drawable;
    }

    private String formatNumber(long value) {
        return NumberFormat.getIntegerInstance(new Locale("pt", "BR")).format(value);
    }

    private String formatDuration(long millis) {
        if (millis < 1000L) return millis + " ms";
        return String.format(Locale.US, "%.2f s", millis / 1000.0);
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
