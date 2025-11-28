# RTL formatting notes

When adding Persian (Farsi) or other right-to-left text to project documentation, use a block-level wrapper to keep alignment consistent with Markdown renderers. Two options that avoid layout shifts:

1. **Div wrapper for full sections**

```html
<div dir="rtl" lang="fa" style="direction: rtl; text-align: right;">
  <h3>نمونه عنوان</h3>
  <p>بدنه متن راست‌چین.</p>
</div>
```

Place the entire RTL section inside the `<div>` so nested headings and paragraphs inherit the direction. Avoid mixing inline RTL tags within LTR lists, which can cause misaligned bullets.

2. **Paragraph-level alignment**

If you only need a few RTL sentences, wrap them in `<p>` tags instead of a full container:

```html
<p dir="rtl" lang="fa" align="right">این یک نمونه متن راست‌چین است.</p>
```

This keeps the rest of the document in left-to-right order while aligning the RTL paragraph.

Additional tips
- Keep Markdown lists outside of RTL blocks or rewrite them as paragraphs; some renderers position list bullets incorrectly when nested in `<div dir="rtl">`.
- Use `lang="fa"` to help screen readers.
- Prefer block-level HTML over inline styling to prevent the right alignment from collapsing or drifting when the document is reflowed.
