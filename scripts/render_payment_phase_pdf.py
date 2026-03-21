from pathlib import Path
from reportlab.lib.pagesizes import A4
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.enums import TA_LEFT
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.pdfbase import pdfmetrics
from reportlab.lib.units import mm

ROOT = Path(r"C:\Users\User\Desktop\refactoring\starvestop-refactoring")
SRC = ROOT / "reports" / "payment-phase-1-contract-cleanup.md"
OUT = Path(r"C:\Users\User\Desktop\payment-phase-1-contract-cleanup.pdf")

font_candidates = [
    r"C:\Windows\Fonts\malgun.ttf",
    r"C:\Windows\Fonts\맑은 고딕.ttf",
]
registered_font = None
for candidate in font_candidates:
    if Path(candidate).exists():
        pdfmetrics.registerFont(TTFont("KoreanFont", candidate))
        registered_font = "KoreanFont"
        break
if registered_font is None:
    registered_font = "Helvetica"

styles = getSampleStyleSheet()
base = ParagraphStyle(
    "Base",
    parent=styles["BodyText"],
    fontName=registered_font,
    fontSize=10.5,
    leading=15,
    alignment=TA_LEFT,
    spaceAfter=4,
)
heading1 = ParagraphStyle(
    "Heading1K",
    parent=styles["Heading1"],
    fontName=registered_font,
    fontSize=18,
    leading=24,
    spaceAfter=10,
)
heading2 = ParagraphStyle(
    "Heading2K",
    parent=styles["Heading2"],
    fontName=registered_font,
    fontSize=13,
    leading=18,
    spaceBefore=8,
    spaceAfter=6,
)

story = []
text = SRC.read_text(encoding="utf-8")
for raw_line in text.splitlines():
    line = raw_line.strip()
    if not line:
        story.append(Spacer(1, 4))
        continue
    if line.startswith("# "):
        story.append(Paragraph(line[2:], heading1))
    elif line.startswith("## "):
        story.append(Paragraph(line[3:], heading2))
    else:
        escaped = (line.replace("&", "&amp;")
                       .replace("<", "&lt;")
                       .replace(">", "&gt;"))
        story.append(Paragraph(escaped, base))

doc = SimpleDocTemplate(str(OUT), pagesize=A4,
                        leftMargin=16*mm, rightMargin=16*mm,
                        topMargin=16*mm, bottomMargin=16*mm)
doc.build(story)
print(str(OUT))
