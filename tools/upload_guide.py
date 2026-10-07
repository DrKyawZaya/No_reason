"""Builds store/upload-guide.html from store/listing.json (published as an Artifact)."""
import html, json
from pathlib import Path

S = Path(__file__).resolve().parent.parent / "store"
d = json.loads((S / "listing.json").read_text())
DOC = "https://docs.google.com/document/d/1YkjCvoSHhPRPAr_cUbq9e3uwRX7PIH8a6wM-qlXNLI4/edit"

def copy(label, key, limit, tall=False):
    v = d[key]
    return f'''<div class="field"><div class="fh"><span class="lbl">{label}</span><span class="cnt">{len(v)} / {limit}</span>
<button type="button" class="cp" data-k="{key}">Copy</button></div>
<pre id="{key}" class="{'tall' if tall else ''}">{html.escape(v)}</pre></div>'''

def row(q, a, note=""):
    return f'<tr><td>{q}</td><td class="ans">{a}</td><td class="note">{note}</td></tr>'

page = f'''<title>စိပ်ပုတီး Play Upload</title>
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Padauk:wght@400;700&family=Fraunces:opsz,wght@9..144,600&family=Source+Sans+3:wght@400;600&display=swap">
<style>
/* Layout: one reading column of numbered Play Console steps, copy-ready text blocks inline */
:root {{
  --bg:#f6efe4; --panel:#fffaf2; --fg:#2a1d12; --muted:#7a6550; --line:#e3d4bf; --accent:#a8661c; --soft:#f1e0c4; --ok:#2f7a4c;
  --display:"Fraunces", Georgia, serif; --body:"Source Sans 3","Padauk",system-ui,sans-serif; --mm:"Padauk","Myanmar Text",system-ui,sans-serif;
}}
@media (prefers-color-scheme: dark) {{ :root:not([data-theme="light"]) {{ --bg:#16110c; --panel:#211912; --fg:#f2e5d1; --muted:#b29c80; --line:#3b2e22; --accent:#eba443; --soft:#4a3317; --ok:#6cc08b; color-scheme:dark }} }}
:root[data-theme="dark"] {{ --bg:#16110c; --panel:#211912; --fg:#f2e5d1; --muted:#b29c80; --line:#3b2e22; --accent:#eba443; --soft:#4a3317; --ok:#6cc08b; color-scheme:dark }}
body {{ background:var(--bg); color:var(--fg); font:16px/1.6 var(--body); padding:32px 16px 64px }}
main {{ max-width:760px; margin:0 auto; display:flex; flex-direction:column; gap:40px }}
h1 {{ font:600 2.3rem/1.15 var(--display); margin:0; text-wrap:balance }}
h2 {{ font:600 1.4rem/1.3 var(--display); margin:0 0 12px; display:flex; gap:12px; align-items:baseline }}
h2 .n {{ color:var(--accent); font-variant-numeric:tabular-nums }}
p, li {{ max-width:65ch }}
.mm, pre {{ font-family:var(--mm) }}
.hero {{ display:grid; grid-template-columns:96px 1fr; gap:20px; align-items:center }}
.hero img {{ border-radius:22px; width:96px }}
.sub {{ color:var(--muted); margin:6px 0 0 }}
.feature {{ border-radius:10px; border:1px solid var(--line) }}
.shots {{ display:grid; grid-template-columns:repeat(4,1fr); gap:10px }}
.shots img {{ border-radius:8px; border:1px solid var(--line) }}
@media (max-width:520px) {{ .shots {{ grid-template-columns:repeat(2,1fr) }} .hero {{ grid-template-columns:64px 1fr }} .hero img {{ width:64px; border-radius:15px }} }}
.field {{ background:var(--panel); border:1px solid var(--line); border-radius:10px; margin:12px 0; min-width:0 }}
.fh {{ display:flex; gap:10px; align-items:center; padding:8px 12px; border-bottom:1px solid var(--line) }}
.lbl {{ font-weight:600; flex:1 }} .cnt {{ color:var(--muted); font-size:.85rem; font-variant-numeric:tabular-nums }}
pre {{ margin:0; padding:12px; white-space:pre-wrap; word-break:break-word; font-size:1rem; line-height:1.8 }}
pre.tall {{ max-height:300px; overflow:auto }}
button.cp {{ font:600 .85rem var(--body); background:var(--accent); color:var(--bg); border:0; border-radius:6px; padding:5px 12px; cursor:pointer }}
button.cp:focus-visible {{ outline:2px solid var(--fg); outline-offset:2px }}
.tw {{ overflow-x:auto }}
table {{ border-collapse:collapse; width:100%; font-size:.95rem }}
td {{ border-top:1px solid var(--line); padding:8px 10px 8px 0; vertical-align:top }}
td.ans {{ font-weight:600; color:var(--ok); white-space:nowrap }} td.note {{ color:var(--muted) }}
.box {{ background:var(--soft); border-radius:10px; padding:14px 16px }}
.box p {{ margin:0 }}
ol, ul {{ padding-left:1.3em; margin:0 }} li {{ margin:4px 0 }}
code {{ background:var(--soft); padding:1px 5px; border-radius:4px; font-size:.9em }}
a {{ color:var(--accent) }}
</style>
<main>
<header class="hero"><img src="icon-512.png" alt="စိပ်ပုတီး app icon">
<div><h1>Uploading <span class="mm">စိပ်ပုတီး</span> to Google Play</h1>
<p class="sub">Version 1.0.0 (code 9) · package <code>com.satepadee.app</code> · every text below is ready to paste.</p></div></header>

<section><h2><span class="n">1</span>Check the release build on your phone</h2>
<p>The release build is smaller and optimised, so install <code>satepadee-1.0.0-release.apk</code> on your Redmi Note 7 or Mi Pad 5 first. Count a few beads, open every screen, set a reminder one minute ahead. If anything looks wrong, tell me before uploading.</p></section>

<section><h2><span class="n">2</span>Create the app</h2>
<p>Play Console → <b>Create app</b>:</p>
<ul><li>App name: <b class="mm">စိပ်ပုတီး</b></li><li>Default language: <b>Burmese – my-MM</b></li><li>App or game: <b>App</b> · Free or paid: <b>Free</b></li><li>Tick both declarations, then <b>Create app</b>.</li></ul></section>

<section><h2><span class="n">3</span>Make the privacy policy public</h2>
<p>The policy is already in your Drive folder <span class="mm">စိပ်ပုတီး</span>. Open <a href="{DOC}">the Google Doc</a>, then <b>File → Share → Publish to web → Publish</b>. Copy the link it gives you (ending in <code>/pub</code>) and paste it into <b>App content → Privacy policy</b>.</p></section>

<section><h2><span class="n">4</span>Store listing</h2>
<p>Grow users → Store presence → <b>Main store listing</b>.</p>
{copy("App name", "name_my", 30)}
{copy("Short description", "short_my", 80)}
{copy("Full description", "full_my", 4000, tall=True)}
<p>Optional: add an English (en-US) translation with these:</p>
{copy("App name (English)", "name_en", 30)}
{copy("Short description (English)", "short_en", 80)}
{copy("Full description (English)", "full_en", 4000, tall=True)}
<h3>Graphics</h3>
<ul><li>App icon: <code>icon-512.png</code> (512×512)</li><li>Feature graphic: <code>feature-graphic-1024x500.png</code></li><li>Phone screenshots: all 7 <code>phone-*.png</code> (1080×2160)</li><li>10-inch tablet screenshots: the 2 <code>tablet-*.png</code></li></ul>
<img class="feature" src="feature-graphic-1024x500.png" alt="Feature graphic">
<div class="shots"><img src="phone-1-home.png" alt="Home"><img src="phone-2-counter.png" alt="Counter"><img src="phone-3-birthday.png" alt="Birth-day wood"><img src="phone-4-stage-done.png" alt="Stage finished"></div>
<p>Store settings: category <b>Lifestyle</b>, contact email <b>kyawzaya00@gmail.com</b>.</p></section>

<section><h2><span class="n">5</span>App content (Policy → App content)</h2>
<div class="tw"><table>
{row("Privacy policy", "Your /pub link", "From step 3")}
{row("Ads", "No", "")}
{row("App access", "All functionality available", "No login")}
{row("Content rating", "Category: Reference, News, or Educational", "Answer No to every question; email kyawzaya00@gmail.com")}
{row("Target audience", "13–15, 16–17, 18+", "Leave under-13 unticked to stay out of the Families programme")}
{row("Data safety", "Collects or shares data? No", "Everything stays on the phone")}
{row("Government app", "No", "")}
{row("Financial features", "None", "")}
{row("Health", "None", "")}
{row("News app", "No", "")}
{row("Advertising ID", "No", "")}
</table></div></section>

<section><h2><span class="n">6</span>Closed test (required before production)</h2>
<div class="box"><p>New personal developer accounts must run a closed test with <b>at least 12 testers for 14 days in a row</b> before Production is unlocked.</p></div>
<ol>
<li>Test and release → Testing → <b>Closed testing</b> → Create track (or use “Alpha”).</li>
<li>Testers: create an email list with 12 or more Gmail addresses (family, friends, monastery members).</li>
<li>Countries: Myanmar plus where your testers live.</li>
<li>Create release → upload <code>satepadee-1.0.0.aab</code>. Accept <b>Play App Signing</b> when asked.</li>
<li>Release notes (my-MM):</li></ol>
{copy("Release notes", "notes_my", 500)}
<ol start="6"><li>Review and roll out. Send testers the <b>opt-in link</b>; each one must join and keep the app installed.</li>
<li>After 14 days: Dashboard → <b>Apply for production</b>, answer the short questionnaire, then promote the same release.</li></ol></section>

<section><h2><span class="n">7</span>Keep the upload key safe</h2>
<p><code>satepadee-upload.jks</code> and <code>keystore.properties</code> (which holds its password) sign every future update. Keep copies in two safe places, for example your own Google Drive and a USB stick. Don't share them. If they are lost, Play Console support can reset the upload key because Google holds the app signing key.</p>
<p class="sub">Upload certificate SHA-256: <code style="word-break:break-all">9A:A6:56:23:73:6C:E3:39:BB:EE:10:77:CD:1D:D1:77:16:FE:ED:13:82:D1:23:94:24:34:62:C7:42:A0:43:1D</code></p></section>
</main>
<script>
document.querySelectorAll("button.cp").forEach(b => b.addEventListener("click", () => {{
  const el = document.getElementById(b.dataset.k), done = () => {{ b.textContent = "Copied"; setTimeout(() => b.textContent = "Copy", 1500) }};
  const select = () => {{ const r = document.createRange(); r.selectNodeContents(el); const s = getSelection(); s.removeAllRanges(); s.addRange(r); b.textContent = "Selected, press copy" }};
  try {{ navigator.clipboard.writeText(el.textContent).then(done, select) }} catch (e) {{ select() }}
}}));
</script>
'''
(S / "upload-guide.html").write_text(page)
print("ok")
