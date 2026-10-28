"""Builds store/update-1.1.0.html (the Play Console steps for the 1.1.0 update) from store/listing.json."""
import html, json
from pathlib import Path

S = Path(__file__).resolve().parent.parent / "store"
d = json.loads((S / "listing.json").read_text())
style = (S / "upload-guide.html").read_text()
style = style[style.index("<style>"):style.index("</style>") + 8]

def copy(label, key, limit, tall=False):
    v = d[key]
    return f'''<div class="field"><div class="fh"><span class="lbl">{label}</span><span class="cnt">{len(v)} / {limit}</span>
<button type="button" class="cp" data-k="{key}">Copy</button></div>
<pre id="{key}" class="{'tall' if tall else ''}">{html.escape(v)}</pre></div>'''

page = f'''<title>စိပ်ပုတီး 1.1.0 Update</title>
<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Padauk:wght@400;700&family=Fraunces:opsz,wght@9..144,600&family=Source+Sans+3:wght@400;600&display=swap">
{style}
<main>
<header class="hero"><img src="icon-512.png" alt="စိပ်ပုတီး app icon">
<div><h1>Updating <span class="mm">စိပ်ပုတီး</span> to 1.1.0</h1>
<p class="sub">Version 1.1.0 (build 12) · same package, same upload key · no new permissions.</p></div></header>

<section><h2><span class="n">1</span>Try the release build first</h2>
<p>Install <code>satepadee-1.1.0-release-test.apk</code> on a phone that does <b>not</b> have the Play version (for example the Mi Pad), because it is signed with a test key. It is the optimised build that goes to Play. Check:</p>
<ul><li>A background picture on Home, the counter, the chart and Settings.</li>
<li>Count a round, then <b class="mm">သိမ်းပြီး ထွက်မည်</b>.</li>
<li>Finish a stage (or use a test phone where one is finished), make a wish card, share it to Viber or Messenger, and save it to Photos.</li>
<li>The reminder at a time one minute ahead.</li></ul></section>

<section><h2><span class="n">2</span>Send me the upload key</h2>
<div class="box"><p>Upload <code>satepadee-upload-key.zip</code> (the zip I sent with version 1.0.0) into the chat. I sign <code>satepadee-1.1.0.aab</code> with it and send it back. Without it Play will refuse the update.</p></div></section>

<section><h2><span class="n">3</span>Upload the update</h2>
<ol>
<li>Play Console → <b>Test and release → Production</b> → <b>Create new release</b>.</li>
<li>Upload <code>satepadee-1.1.0.aab</code>. The release name should show <b>12 (1.1.0)</b>.</li>
<li>Paste the release notes:</li></ol>
{copy("Release notes (my-MM)", "notes_my_1_1_0", 500)}
{copy("Release notes (en-US, if you added English)", "notes_en_1_1_0", 500)}
<ol start="4"><li><b>Next → Save → Send for review</b>. Updates are usually reviewed within a few hours to a couple of days, then reach users automatically.</li></ol>
<p class="sub">Want a safety step first? Create the same release on <b>Internal testing</b>, install it from Play on your phone, then use <b>Promote release → Production</b>.</p></section>

<section><h2><span class="n">4</span>Refresh the store listing</h2>
<p>Grow users → Store presence → <b>Main store listing</b>.</p>
{copy("Full description (my-MM)", "full_my", 4000, tall=True)}
{copy("Full description (en-US)", "full_en", 4000, tall=True)}
<p><b>Phone screenshots:</b> remove the old ones and upload the 8 new <code>phone-*.png</code> in order. <b>Tablet:</b> replace with the 2 new <code>tablet-*.png</code>.</p>
<div class="shots"><img src="phone-1-home.png" alt="Home with Shwedagon background"><img src="phone-2-counter.png" alt="Counter with Kyaiktiyo background"><img src="phone-3-wish-card.png" alt="A made wish card"><img src="phone-4-chart.png" alt="Chart and wish card collection"></div>
<div class="box"><p><b>AI label:</b> the new screenshots show the Canva AI background pictures. On the listing's <b>AI asset declaration</b> step, choose <b>Label assets as created or edited using AI</b> and mark the screenshots that show a picture (all except <code>phone-7-birthday</code>). The icon and feature graphic are unchanged and drawn by code.</p></div></section>

<section><h2><span class="n">5</span>Nothing else changes</h2>
<ul><li><b>Data safety:</b> still "no data collected or shared". Wish cards stay on the phone and are only shared when the user taps Share.</li>
<li><b>Permissions:</b> the same four as 1.0.0.</li>
<li><b>Content rating, target audience, ads, privacy policy:</b> unchanged.</li></ul></section>
</main>
<script>
document.querySelectorAll("button.cp").forEach(b => b.addEventListener("click", () => {{
  const el = document.getElementById(b.dataset.k), done = () => {{ b.textContent = "Copied"; setTimeout(() => b.textContent = "Copy", 1500) }};
  const select = () => {{ const r = document.createRange(); r.selectNodeContents(el); const s = getSelection(); s.removeAllRanges(); s.addRange(r); b.textContent = "Selected, press copy" }};
  try {{ navigator.clipboard.writeText(el.textContent).then(done, select) }} catch (e) {{ select() }}
}}));
</script>
'''
(S / "update-1.1.0.html").write_text(page)
print("ok")
