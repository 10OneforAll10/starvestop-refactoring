from pathlib import Path
from reportlab.lib.pagesizes import A4
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.enums import TA_LEFT
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.pdfbase import pdfmetrics
from reportlab.lib.units import mm
from reportlab.lib import colors

ROOT = Path(r"C:\Users\User\Desktop\refactoring\starvestop-refactoring")
SRC = ROOT / "reports" / "payment-phase-4-pg-client-separation.md"
OUT = Path(r"C:\Users\User\Desktop\payment-phase-4-pg-client-separation.pdf")

font_candidates = [r"C:\Windows\Fonts\malgun.ttf", r"C:\Windows\Fonts\맑은 고딕.ttf"]
registered_font = None
for candidate in font_candidates:
    if Path(candidate).exists():
        pdfmetrics.registerFont(TTFont("KoreanFont", candidate))
        registered_font = "KoreanFont"
        break
if registered_font is None:
    registered_font = "Helvetica"

styles = getSampleStyleSheet()
base = ParagraphStyle("Base", parent=styles["BodyText"], fontName=registered_font, fontSize=10.5, leading=15, alignment=TA_LEFT, spaceAfter=4)
heading1 = ParagraphStyle("Heading1K", parent=styles["Heading1"], fontName=registered_font, fontSize=18, leading=24, spaceAfter=10)
heading2 = ParagraphStyle("Heading2K", parent=styles["Heading2"], fontName=registered_font, fontSize=13, leading=18, spaceBefore=8, spaceAfter=6)
small = ParagraphStyle("Small", parent=base, fontSize=9.5, leading=13)

def esc(text: str) -> str:
    return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

story = []
lines = SRC.read_text(encoding="utf-8").splitlines()
i = 0
while i < len(lines):
    line = lines[i].strip()
    if not line:
        story.append(Spacer(1, 4))
        i += 1
        continue
    if line.startswith("# "):
        story.append(Paragraph(esc(line[2:]), heading1))
        i += 1
        continue
    if line.startswith("## "):
        story.append(Paragraph(esc(line[3:]), heading2))
        i += 1
        continue
    if line.startswith("|"):
        table_lines = []
        while i < len(lines) and lines[i].strip().startswith("|"):
            table_lines.append(lines[i].strip())
            i += 1
        rows = []
        for idx, tline in enumerate(table_lines):
            cells = [c.strip() for c in tline.strip("|").split("|")]
            if idx == 1 and all(set(c) <= {"-", ":"} for c in cells):
                continue
            rows.append([Paragraph(esc(cell), small) for cell in cells])
        if rows:
            tbl = Table(rows, repeatRows=1)
            tbl.setStyle(TableStyle([
                ("FONTNAME", (0, 0), (-1, -1), registered_font),
                ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#E9EEF5")),
                ("GRID", (0, 0), (-1, -1), 0.5, colors.HexColor("#AAB4C3")),
                ("VALIGN", (0, 0), (-1, -1), "TOP"),
                ("PADDING", (0, 0), (-1, -1), 6),
            ]))
            story.append(tbl)
            story.append(Spacer(1, 6))
        continue
    story.append(Paragraph(esc(line), base))
    i += 1

doc = SimpleDocTemplate(str(OUT), pagesize=A4, leftMargin=16*mm, rightMargin=16*mm, topMargin=16*mm, bottomMargin=16*mm)
doc.build(story)
print(str(OUT))
