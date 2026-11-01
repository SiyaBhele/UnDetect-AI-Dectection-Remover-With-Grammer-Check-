import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

    private static final int PORT = 8080;
    private static final String OUTPUT_DIR = "output";
    private static final URI GRAMMAR_API = URI.create("https://api.languagetool.org/v2/check");
    private static final int MAX_GRAMMAR_TEXT_LENGTH = 20_000;
    private static final String PAGE_STYLES = """
        :root {
            color-scheme: light;
            --ink: #172554;
            --muted: #64748b;
            --line: #e2e8f0;
            --paper: #ffffff;
        }
        * { box-sizing: border-box; }
        body {
            min-height: 100vh;
            margin: 0;
            color: var(--ink);
            background:
                radial-gradient(ellipse at 5% 0%, rgba(196, 181, 253, .48), transparent 34rem),
                radial-gradient(ellipse at 95% 8%, rgba(103, 232, 249, .42), transparent 30rem),
                #f5f7ff;
            font-family: Inter, ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
        }
        button, textarea { font: inherit; }
        .topbar {
            display: flex;
            align-items: center;
            justify-content: space-between;
            gap: 1rem;
            padding: 1rem max(1.25rem, calc((100vw - 1120px) / 2));
            border-bottom: 1px solid rgba(226, 232, 240, .8);
            background: rgba(255, 255, 255, .76);
            backdrop-filter: blur(14px);
        }
        .brand { display: flex; align-items: center; gap: .75rem; }
        .brand-mark {
            display: grid;
            width: 2.7rem;
            height: 2.7rem;
            place-items: center;
            border-radius: .9rem;
            color: #fff;
            background: linear-gradient(140deg, #7c3aed, #2563eb 65%, #06b6d4);
            box-shadow: 0 8px 18px rgba(79, 70, 229, .25);
            font-weight: 900;
        }
        .brand-name { font-size: 1.08rem; font-weight: 800; letter-spacing: -.04em; }
        .brand-caption { margin-top: .12rem; color: var(--muted); font-size: .75rem; }
        .local-pill {
            padding: .45rem .75rem;
            border: 1px solid #bbf7d0;
            border-radius: 999px;
            color: #15803d;
            background: #f0fdf4;
            font-size: .76rem;
            font-weight: 700;
        }
        .shell { width: min(1120px, calc(100% - 2rem)); margin: 0 auto; padding: 2rem 0 4rem; }
        .hero {
            position: relative;
            overflow: hidden;
            padding: clamp(1.6rem, 5vw, 3.3rem);
            border-radius: 1.8rem;
            color: white;
            background: linear-gradient(120deg, #4f46e5 0%, #7c3aed 48%, #c026d3 100%);
            box-shadow: 0 24px 60px rgba(91, 67, 207, .22);
        }
        .hero::after {
            position: absolute;
            top: -8rem;
            right: -3rem;
            width: 24rem;
            height: 24rem;
            border: 1px solid rgba(255,255,255,.2);
            border-radius: 50%;
            box-shadow: 0 0 0 2.8rem rgba(255,255,255,.06), 0 0 0 5.6rem rgba(255,255,255,.04);
            content: "";
            pointer-events: none;
        }
        .eyebrow {
            display: inline-flex;
            align-items: center;
            gap: .5rem;
            padding: .38rem .72rem;
            border: 1px solid rgba(255,255,255,.3);
            border-radius: 999px;
            background: rgba(255,255,255,.12);
            font-size: .76rem;
            font-weight: 700;
        }
        .eyebrow-dot { width: .48rem; height: .48rem; border-radius: 50%; background: #86efac; }
        .hero h1 { max-width: 720px; margin: 1.2rem 0 .75rem; font-size: clamp(2.15rem, 5vw, 3.7rem); line-height: 1.05; letter-spacing: -.055em; }
        .hero p { max-width: 660px; margin: 0; color: rgba(255,255,255,.84); font-size: 1.02rem; line-height: 1.7; }
        .hero-note { margin-top: 1rem !important; font-size: .84rem !important; }
        .feature-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: .85rem; margin: 1.2rem 0; }
        .feature-card {
            display: flex;
            align-items: center;
            gap: .8rem;
            min-height: 5.1rem;
            padding: 1rem;
            border: 1px solid rgba(226,232,240,.9);
            border-radius: 1rem;
            background: rgba(255,255,255,.88);
            box-shadow: 0 8px 22px rgba(30,41,59,.04);
        }
        .feature-icon { display: grid; width: 2.35rem; height: 2.35rem; flex: 0 0 auto; place-items: center; border-radius: .8rem; font-size: 1.1rem; }
        .feature-card:nth-child(1) .feature-icon { color: #6d28d9; background: #ede9fe; }
        .feature-card:nth-child(2) .feature-icon { color: #0e7490; background: #cffafe; }
        .feature-card:nth-child(3) .feature-icon { color: #15803d; background: #dcfce7; }
        .feature-card:nth-child(4) .feature-icon { color: #c2410c; background: #ffedd5; }
        .feature-title { font-size: .82rem; font-weight: 800; }
        .feature-detail { margin-top: .18rem; color: var(--muted); font-size: .7rem; line-height: 1.4; }
        .panel {
            overflow: hidden;
            border: 1px solid rgba(226,232,240,.95);
            border-radius: 1.3rem;
            background: var(--paper);
            box-shadow: 0 15px 40px rgba(30,41,59,.07);
        }
        .panel-head { display: flex; align-items: center; justify-content: space-between; gap: 1rem; padding: 1.2rem 1.35rem; border-bottom: 1px solid #eef2ff; }
        .panel-title { font-size: 1rem; font-weight: 800; }
        .panel-subtitle { margin-top: .2rem; color: var(--muted); font-size: .78rem; }
        .input-wrap { padding: 1.2rem 1.35rem; }
        .text-input, .text-output {
            display: block;
            width: 100%;
            min-height: 260px;
            padding: 1rem;
            resize: vertical;
            border: 1px solid #dbe3f0;
            border-radius: .9rem;
            outline: none;
            color: #1e293b;
            background: #fbfcff;
            font: .9rem/1.65 ui-monospace, SFMono-Regular, Menlo, monospace;
            transition: border-color .2s, box-shadow .2s;
        }
        .text-input:focus { border-color: #8b5cf6; box-shadow: 0 0 0 4px rgba(139,92,246,.12); }
        .text-output { min-height: 320px; background: #f8faff; }
        .editor-meta { display: flex; justify-content: space-between; gap: .8rem; padding-top: .65rem; color: var(--muted); font-size: .75rem; }
        .controls { display: flex; align-items: center; justify-content: space-between; gap: 1rem; flex-wrap: wrap; margin-top: 1rem; }
        .check-row { display: flex; align-items: flex-start; gap: .65rem; max-width: 650px; color: #475569; font-size: .82rem; line-height: 1.5; }
        .check-row input { margin-top: .18rem; accent-color: #7c3aed; }
        .primary-btn, .secondary-btn, .tab-btn {
            display: inline-flex;
            align-items: center;
            justify-content: center;
            gap: .55rem;
            border: 0;
            border-radius: .8rem;
            cursor: pointer;
            font-weight: 800;
            transition: transform .15s, box-shadow .15s, background .15s;
        }
        .primary-btn { padding: .9rem 1.25rem; color: white; background: linear-gradient(110deg, #6d28d9, #4f46e5 65%, #0891b2); box-shadow: 0 8px 18px rgba(79,70,229,.23); }
        .primary-btn:hover { transform: translateY(-1px); box-shadow: 0 12px 24px rgba(79,70,229,.3); }
        .secondary-btn { padding: .65rem .9rem; border: 1px solid var(--line); color: #475569; background: #fff; font-size: .8rem; }
        .secondary-btn:hover { background: #f8fafc; }
        .tabs { display: flex; gap: .4rem; padding: .35rem; border: 1px solid #e7eaf3; border-radius: .85rem; background: #f5f6fb; }
        .tab-btn { padding: .58rem .85rem; color: #64748b; background: transparent; font-size: .8rem; }
        .tab-btn.active { color: #5b21b6; background: white; box-shadow: 0 1px 5px rgba(15,23,42,.12); }
        .compare-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; }
        .compare-label { display: flex; align-items: center; gap: .45rem; margin-bottom: .55rem; font-size: .82rem; font-weight: 800; }
        .compare-label.before { color: #b45309; }
        .compare-label.after { color: #15803d; }
        .status-note { margin: 1rem 1.35rem 0; padding: .8rem 1rem; border: 1px solid #dbeafe; border-radius: .8rem; color: #1e40af; background: #eff6ff; font-size: .82rem; line-height: 1.5; }
        .suggestions { margin: 1rem 1.35rem; padding: 1rem 1.1rem; border: 1px solid #fde68a; border-radius: .9rem; background: #fffbeb; color: #713f12; font-size: .84rem; line-height: 1.6; }
        .suggestions h2 { margin: 0 0 .5rem; font-size: .95rem; }
        .suggestions li + li { margin-top: .45rem; }
        .result-meta { display: flex; justify-content: space-between; gap: .7rem; flex-wrap: wrap; padding: .8rem 1.35rem 1.2rem; color: var(--muted); font-size: .76rem; }
        .result-actions { display: flex; justify-content: space-between; align-items: center; gap: 1rem; flex-wrap: wrap; padding: 1rem 1.35rem; border-top: 1px solid #eef2ff; background: #fafbff; }
        .footer-note { padding: 1.2rem .25rem; color: #64748b; text-align: center; font-size: .76rem; line-height: 1.6; }
        .hidden { display: none !important; }
        @media (max-width: 760px) {
            .feature-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
            .compare-grid { grid-template-columns: 1fr; }
            .topbar { padding-right: 1rem; padding-left: 1rem; }
        }
        @media (max-width: 480px) {
            .shell { width: min(100% - 1rem, 1120px); padding-top: .7rem; }
            .hero { border-radius: 1.2rem; }
            .feature-grid { gap: .55rem; }
            .feature-card { align-items: flex-start; padding: .72rem; }
            .feature-icon { width: 2rem; height: 2rem; }
            .panel-head, .input-wrap { padding-right: .9rem; padding-left: .9rem; }
            .controls { align-items: stretch; }
            .primary-btn { width: 100%; }
            .local-pill { font-size: .68rem; }
        }
        """;
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    public static void main(String[] args) throws IOException {
        Files.createDirectories(Paths.get(OUTPUT_DIR));

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/", new GUIHandler());
        server.createContext("/clean", new CleanHandler());
        server.setExecutor(null);

        System.out.println("==================================================");
        System.out.println(" Call Text Cleaner Localhost GUI Running");
        System.out.println(" Open your browser at: http://localhost:" + PORT);
        System.out.println("==================================================");

        server.start();
    }

    static class GUIHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String html = """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1">
                    <title>Call Text Cleaner</title>
                    <link rel="preconnect" href="https://fonts.googleapis.com">
                    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
                    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap" rel="stylesheet">
                    <style>__STYLES__</style>
                </head>
                <body>
                    <header class="topbar">
                        <div class="brand">
                            <div class="brand-mark">A</div>
                            <div><div class="brand-name">Call Text Cleaner</div><div class="brand-caption">Thoughtful text cleanup</div></div>
                        </div>
                        <span class="local-pill">● Local cleanup</span>
                    </header>
                    <main class="shell">
                        <section class="hero">
                            <span class="eyebrow"><span class="eyebrow-dot"></span> A clearer writing workspace</span>
                            <h1>Clean up text.<br>Keep your meaning.</h1>
                            <p>Remove selected hidden formatting artifacts and execution-log lines, then review exactly what changed.</p>
                            <p class="hero-note">This tool does not identify AI-written text or predict AI-detector results.</p>
                        </section>
                        <section class="feature-grid" aria-label="What this tool does">
                            <article class="feature-card"><span class="feature-icon">✳</span><div><div class="feature-title">Unicode cleanup</div><div class="feature-detail">Selected invisible spaces and marks</div></div></article>
                            <article class="feature-card"><span class="feature-icon">⌘</span><div><div class="feature-title">Log-line cleanup</div><div class="feature-detail">Phase and execution headers</div></div></article>
                            <article class="feature-card"><span class="feature-icon">⇄</span><div><div class="feature-title">Before &amp; after</div><div class="feature-detail">Compare original with cleaned text</div></div></article>
                            <article class="feature-card"><span class="feature-icon">✓</span><div><div class="feature-title">Grammar suggestions</div><div class="feature-detail">Optional LanguageTool review</div></div></article>
                        </section>
                        <section class="panel">
                            <div class="panel-head">
                                <div><div class="panel-title">Your document</div><div class="panel-subtitle">Paste text to clean up formatting artifacts.</div></div>
                                <button type="button" class="secondary-btn" id="sampleButton">Try a sample</button>
                            </div>
                            <form action="/clean" method="POST" class="input-wrap">
                                <textarea class="text-input" id="documentText" name="documentText" placeholder="Paste or type your text here..." required></textarea>
                                <div class="editor-meta"><span id="inputCount">0 characters · 0 words</span><span>Text stays local unless grammar check is selected.</span></div>
                                <div class="controls">
                                    <label class="check-row">
                                        <input type="checkbox" name="grammarCheck" value="true">
                                        <span>Check grammar with the free public LanguageTool API. Your cleaned text will be sent to this third-party service (20,000 character limit); suggestions are never applied automatically.</span>
                                    </label>
                                    <button type="submit" class="primary-btn">Clean and compare <span aria-hidden="true">→</span></button>
                                </div>
                            </form>
                        </section>
                        <p class="footer-note">Local cleanup handles only the artifacts listed above. Grammar checking is optional and separate.</p>
                    </main>
                    <script>
                        const input = document.getElementById('documentText');
                        const count = document.getElementById('inputCount');
                        function updateCount() {
                            const value = input.value;
                            const words = value.trim() ? value.trim().split(/\\s+/).length : 0;
                            count.textContent = value.length + ' characters · ' + words + ' words';
                        }
                        input.addEventListener('input', updateCount);
                        document.getElementById('sampleButton').addEventListener('click', () => {
                            input.value = 'Phase 1: Example log\\nA copied sentence with a zero-width character: here​there.\\nExecution Log: temporary trace\\nThis final sentence stays unchanged.';
                            updateCount();
                            input.focus();
                        });
                    </script>
                </body>
                </html>
                """.replace("__STYLES__", PAGE_STYLES);

            byte[] response = html.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        }
    }

    static class CleanHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(451, -1);
                return;
            }

            InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
            BufferedReader br = new BufferedReader(isr);
            StringBuilder formData = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                formData.append(line);
            }

            String rawText = "";
            Map<String, String> parsedParams = parseFormData(formData.toString());
            if (parsedParams.containsKey("documentText")) {
                rawText = parsedParams.get("documentText").replace("\r\n", "\n").replace('\r', '\n');
            }

            String cleaned = cleanText(rawText);
            boolean grammarCheck = "true".equals(parsedParams.get("grammarCheck"));
            List<GrammarSuggestion> suggestions = List.of();
            if (grammarCheck) {
                if (cleaned.length() > MAX_GRAMMAR_TEXT_LENGTH) {
                    sendError(exchange, 413, "LanguageTool checks are limited to 20,000 characters. No output file was saved.");
                    return;
                }
                try {
                    suggestions = checkGrammar(cleaned);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    sendError(exchange, 503, "The grammar check was interrupted. No output file was saved; retry or submit without the grammar check.");
                    return;
                } catch (IOException e) {
                    System.err.println("LanguageTool grammar check failed: " + e.getMessage());
                    sendError(exchange, 502, "The grammar service could not complete the check. No output file was saved; retry or submit without the grammar check.");
                    return;
                }
            }

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fileName = "cleaned_output_" + timestamp + ".txt";
            Path filePath = Paths.get(OUTPUT_DIR, fileName);
            Files.writeString(filePath, cleaned, StandardCharsets.UTF_8);
            String checkStatus = grammarCheck
                ? "LanguageTool checked the text. Suggestions are listed below and have not been applied automatically."
                : "Only local cleanup was performed; no text was sent to LanguageTool.";
            String suggestionHtml = renderSuggestions(suggestions, cleaned);

            String responseHtml = """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1">
                    <title>Call Text Cleaner | Compare Changes</title>
                    <link rel="preconnect" href="https://fonts.googleapis.com">
                    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
                    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap" rel="stylesheet">
                    <style>__STYLES__</style>
                </head>
                <body>
                    <header class="topbar">
                        <div class="brand">
                            <div class="brand-mark">A</div>
                            <div><div class="brand-name">Call Text Cleaner</div><div class="brand-caption">Text cleanup results</div></div>
                        </div>
                        <span class="local-pill">✓ Review your changes</span>
                    </header>
                    <main class="shell">
                        <section class="hero">
                            <span class="eyebrow"><span class="eyebrow-dot"></span> Cleanup complete</span>
                            <h1>Your text, side by side.</h1>
                            <p>Review the cleaned version or compare it directly with your original. Your wording is preserved except for the listed formatting cleanup.</p>
                        </section>
                        <section class="panel" style="margin-top:1.2rem">
                            <div class="panel-head">
                                <div><div class="panel-title">Document review</div><div class="panel-subtitle">Switch tabs to inspect the result or compare both versions.</div></div>
                                <div class="tabs" role="tablist" aria-label="Text result views">
                                    <button type="button" class="tab-btn active" id="cleanTab" role="tab" aria-selected="true">Cleaned text</button>
                                    <button type="button" class="tab-btn" id="compareTab" role="tab" aria-selected="false">Compare changes</button>
                                </div>
                            </div>
                            <div id="cleanView" class="input-wrap" role="tabpanel">
                                <div class="compare-label after">✓ Cleaned text</div>
                                <textarea id="cleanedText" class="text-output" readonly>__CLEANED_OUTPUT__</textarea>
                                <div class="editor-meta"><span>__CLEANED_COUNT__ characters</span><span>Saved as __OUTPUT_NAME__</span></div>
                            </div>
                            <div id="compareView" class="input-wrap hidden" role="tabpanel">
                                <div class="compare-grid">
                                    <div><div class="compare-label before">● Original text</div><textarea class="text-output" readonly>__ORIGINAL_TEXT__</textarea></div>
                                    <div><div class="compare-label after">✓ Cleaned text</div><textarea class="text-output" readonly>__CLEANED_COMPARE__</textarea></div>
                                </div>
                                <div class="editor-meta"><span>Original: __ORIGINAL_COUNT__ characters</span><span>Cleaned: __CLEANED_COUNT_COMPARE__ characters</span></div>
                            </div>
                            <p class="status-note">__CHECK_STATUS__</p>
                            __SUGGESTIONS__
                            <div class="result-actions">
                                <span class="panel-subtitle">Local cleanup does not determine whether text was AI-written.</span>
                                <div style="display:flex;gap:.6rem;flex-wrap:wrap">
                                    <button type="button" class="secondary-btn" id="copyButton">Copy cleaned text</button>
                                    <a class="primary-btn" href="/" style="text-decoration:none">Clean another document →</a>
                                </div>
                            </div>
                        </section>
                        <p class="footer-note">Grammar suggestions, if requested, are advisory and have not been applied to your text.</p>
                    </main>
                    <script>
                        const cleanTab = document.getElementById('cleanTab');
                        const compareTab = document.getElementById('compareTab');
                        const cleanView = document.getElementById('cleanView');
                        const compareView = document.getElementById('compareView');
                        function selectView(showCompare) {
                            cleanView.classList.toggle('hidden', showCompare);
                            compareView.classList.toggle('hidden', !showCompare);
                            cleanTab.classList.toggle('active', !showCompare);
                            compareTab.classList.toggle('active', showCompare);
                            cleanTab.setAttribute('aria-selected', String(!showCompare));
                            compareTab.setAttribute('aria-selected', String(showCompare));
                        }
                        cleanTab.addEventListener('click', () => selectView(false));
                        compareTab.addEventListener('click', () => selectView(true));
                        document.getElementById('copyButton').addEventListener('click', async () => {
                            try {
                                await navigator.clipboard.writeText(document.getElementById('cleanedText').value);
                                document.getElementById('copyButton').textContent = 'Copied!';
                            } catch (error) {
                                document.getElementById('copyButton').textContent = 'Copy unavailable';
                            }
                        });
                    </script>
                </body>
                </html>
                """.replace("__STYLES__", PAGE_STYLES)
                   .replace("__CLEANED_OUTPUT__", escapeHtml(cleaned))
                   .replace("__CLEANED_COMPARE__", escapeHtml(cleaned))
                   .replace("__ORIGINAL_TEXT__", escapeHtml(rawText))
                   .replace("__CLEANED_COUNT__", Integer.toString(cleaned.length()))
                   .replace("__CLEANED_COUNT_COMPARE__", Integer.toString(cleaned.length()))
                   .replace("__ORIGINAL_COUNT__", Integer.toString(rawText.length()))
                   .replace("__OUTPUT_NAME__", escapeHtml(fileName))
                   .replace("__CHECK_STATUS__", escapeHtml(checkStatus))
                   .replace("__SUGGESTIONS__", suggestionHtml);

            byte[] response = responseHtml.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        }

        private String cleanText(String input) {
            String cleaned = input.replaceAll("[\\u200B-\\u200D\\uFEFF\\u202F\\u00A0\\u00AD]", " ");
            cleaned = cleaned.replaceAll("\\*\\*([^*]+)\\*\\*:", "$1:");
            cleaned = cleaned.replaceAll("(?im)^[ \\t]*(?:Phase \\d+:|Execution Log:)[^\\r\\n]*(?:\\R|$)", "");
            cleaned = cleaned.replaceAll("(?m)^[ \\t]+|[ \\t]+$", "");
            cleaned = cleaned.replaceAll("[ \\t]{2,}", " ");
            cleaned = cleaned.replaceAll("\\n{3,}", "\n\n");
            return cleaned.trim();
        }

        private List<GrammarSuggestion> checkGrammar(String text) throws IOException, InterruptedException {
            String form = "text=" + URLEncoder.encode(text, StandardCharsets.UTF_8)
                + "&language=auto";
            HttpRequest request = HttpRequest.newBuilder(GRAMMAR_API)
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .header("User-Agent", "Call-Text-Cleaner/1.0")
                .POST(HttpRequest.BodyPublishers.ofString(form, StandardCharsets.UTF_8))
                .build();
            HttpResponse<String> response = HTTP_CLIENT.send(
                request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IOException("LanguageTool returned HTTP " + response.statusCode());
            }
            return parseSuggestions(response.body(), text.length());
        }

        private List<GrammarSuggestion> parseSuggestions(String json, int textLength) throws IOException {
            int matchesKey = json.indexOf("\"matches\"");
            int arrayStart = matchesKey < 0 ? -1 : json.indexOf('[', matchesKey);
            if (arrayStart < 0) {
                throw new IOException("LanguageTool response did not contain grammar matches");
            }

            List<GrammarSuggestion> suggestions = new ArrayList<>();
            int cursor = arrayStart + 1;
            while (cursor < json.length()) {
                while (cursor < json.length()
                    && (Character.isWhitespace(json.charAt(cursor)) || json.charAt(cursor) == ',')) {
                    cursor++;
                }
                if (cursor >= json.length() || json.charAt(cursor) == ']') {
                    return suggestions;
                }
                if (json.charAt(cursor) != '{') {
                    throw new IOException("Invalid LanguageTool matches response");
                }

                int objectEnd = findJsonObjectEnd(json, cursor);
                String match = json.substring(cursor, objectEnd + 1);
                int offset = readIntegerProperty(match, "offset");
                int length = readIntegerProperty(match, "length");
                String message = readStringProperty(match, "message");
                List<String> replacements = readReplacements(match);
                if (offset > textLength || length > textLength - offset) {
                    throw new IOException("LanguageTool returned an invalid suggestion range");
                }
                suggestions.add(new GrammarSuggestion(offset, length, message, replacements));
                cursor = objectEnd + 1;
            }
            throw new IOException("Incomplete LanguageTool matches response");
        }

        private int findJsonObjectEnd(String json, int start) throws IOException {
            int depth = 0;
            boolean inString = false;
            boolean escaped = false;
            for (int i = start; i < json.length(); i++) {
                char current = json.charAt(i);
                if (inString) {
                    if (escaped) {
                        escaped = false;
                    } else if (current == '\\') {
                        escaped = true;
                    } else if (current == '"') {
                        inString = false;
                    }
                } else if (current == '"') {
                    inString = true;
                } else if (current == '{') {
                    depth++;
                } else if (current == '}' && --depth == 0) {
                    return i;
                }
            }
            throw new IOException("Incomplete LanguageTool match object");
        }

        private int readIntegerProperty(String json, String name) throws IOException {
            Matcher matcher = Pattern.compile("\"" + Pattern.quote(name) + "\"\\s*:\\s*(\\d+)")
                .matcher(json);
            if (!matcher.find()) {
                throw new IOException("LanguageTool match is missing " + name);
            }
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                throw new IOException("Invalid LanguageTool " + name, e);
            }
        }

        private String readStringProperty(String json, String name) throws IOException {
            Matcher matcher = Pattern.compile(
                "\"" + Pattern.quote(name) + "\"\\s*:\\s*(\"(?:\\\\.|[^\"\\\\])*\")"
            ).matcher(json);
            return matcher.find() ? decodeJsonString(matcher.group(1)) : "";
        }

        private List<String> readReplacements(String match) throws IOException {
            int replacementsKey = match.indexOf("\"replacements\"");
            int arrayStart = replacementsKey < 0 ? -1 : match.indexOf('[', replacementsKey);
            if (arrayStart < 0) {
                return List.of();
            }
            List<String> replacements = new ArrayList<>();
            int cursor = arrayStart + 1;
            while (cursor < match.length()) {
                while (cursor < match.length()
                    && (Character.isWhitespace(match.charAt(cursor)) || match.charAt(cursor) == ',')) {
                    cursor++;
                }
                if (cursor >= match.length() || match.charAt(cursor) == ']') {
                    return replacements;
                }
                if (match.charAt(cursor) != '{') {
                    throw new IOException("Invalid LanguageTool replacement response");
                }
                int objectEnd = findJsonObjectEnd(match, cursor);
                String value = readStringProperty(match.substring(cursor, objectEnd + 1), "value");
                replacements.add(value);
                cursor = objectEnd + 1;
            }
            throw new IOException("Incomplete LanguageTool replacements response");
        }

        private String decodeJsonString(String quoted) throws IOException {
            StringBuilder result = new StringBuilder();
            for (int i = 1; i < quoted.length() - 1; i++) {
                char current = quoted.charAt(i);
                if (current != '\\') {
                    result.append(current);
                    continue;
                }
                if (++i >= quoted.length() - 1) {
                    throw new IOException("Invalid escaped string in LanguageTool response");
                }
                char escaped = quoted.charAt(i);
                switch (escaped) {
                    case '"', '\\', '/' -> result.append(escaped);
                    case 'b' -> result.append('\b');
                    case 'f' -> result.append('\f');
                    case 'n' -> result.append('\n');
                    case 'r' -> result.append('\r');
                    case 't' -> result.append('\t');
                    case 'u' -> {
                        if (i + 4 >= quoted.length()) {
                            throw new IOException("Invalid Unicode escape in LanguageTool response");
                        }
                        try {
                            result.append((char) Integer.parseInt(quoted.substring(i + 1, i + 5), 16));
                        } catch (NumberFormatException e) {
                            throw new IOException("Invalid Unicode escape in LanguageTool response", e);
                        }
                        i += 4;
                    }
                    default -> throw new IOException("Invalid escape in LanguageTool response");
                }
            }
            return result.toString();
        }

        private String renderSuggestions(List<GrammarSuggestion> suggestions, String text) {
            if (suggestions.isEmpty()) {
                return "";
            }
            StringBuilder html = new StringBuilder("<h2>Grammar suggestions</h2><ul>");
            for (GrammarSuggestion suggestion : suggestions) {
                String original = text.substring(suggestion.offset(), suggestion.offset() + suggestion.length());
                html.append("<li><strong>")
                    .append(escapeHtml(original))
                    .append("</strong>: ")
                    .append(escapeHtml(suggestion.message()));
                if (!suggestion.replacements().isEmpty()) {
                    html.append(" Suggested: ");
                    for (int i = 0; i < suggestion.replacements().size(); i++) {
                        if (i > 0) {
                            html.append(" / ");
                        }
                        html.append("<code>")
                            .append(escapeHtml(suggestion.replacements().get(i)))
                            .append("</code>");
                    }
                }
                html.append("</li>");
            }
            return html.append("</ul>").toString();
        }

        private void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
            String html = "<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"UTF-8\">"
                + "<title>Text check failed</title></head><body><h1>Text check failed</h1><p>"
                + escapeHtml(message)
                + "</p><p><a href=\"/\">Return to the cleaner</a></p></body></html>";
            byte[] response = html.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(statusCode, response.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(response);
            }
        }

        private record GrammarSuggestion(int offset, int length, String message, List<String> replacements) {}

        private Map<String, String> parseFormData(String formData) {
            Map<String, String> map = new HashMap<>();
            String[] pairs = formData.split("&");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=", 2);
                if (keyValue.length == 2) {
                    map.put(keyValue[0], URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8));
                }
            }
            return map;
        }

        private String escapeHtml(String input) {
            return input.replace("&", "&amp;")
                        .replace("<", "&lt;")
                        .replace(">", "&gt;");
        }
    }
}
