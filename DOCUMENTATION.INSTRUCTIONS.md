# Documentation Generation Instructions

This document explains how to generate all project documentation, including Java backend, JavaScript frontend, and CSS style guide, using Gradle, JSDoc, and KSS. All generated documentation will be placed in the /build/docs directory.

---

## 1. Javadoc (Java backend)

The backend code is documented using Javadoc.

### Notes

- Ensure that all Java files have proper Javadoc comments.
- If you see HTML errors during generation (e.g., malformed HTML), replace < and > in author emails with &lt; and &gt;:

@author Pablo Gimeno Ramallo &lt;pgrdaw@gmail.com&gt;

---

## 2. JSDoc (JavaScript frontend)

The frontend JavaScript files are documented using JSDoc.

### Notes

- Ensure that all JS files have proper JSDoc comments with @file, @author, @version, etc.
- Example JSDoc header:

/**
* gallery.js
*
* @file Handles image gallery behavior including loading, sorting, and filtering.
* @author Pablo Gimeno Ramallo
* @version 1.0
* @since 2026-01-01
  */

---

## 3. KSS (CSS style guide)

CSS files are documented using KSS.

### Pre-requisites

- Node.js and npx installed
- KSS installed (locally via npx kss)

### Notes

- All CSS files must have KSS-style comments with @styleguide, @description, @modifier, etc.
- Example KSS comment:

/**
* Button component
*
* @styleguide Button
* @description Defines primary and secondary buttons used across the UI.
* @modifier .button--primary Applies the primary color scheme
* @modifier .button--disabled Applies disabled styling
*
* @author Pablo Gimeno Ramallo
* @since 2026-01-01
  */

- homepage.md should contain an introductory overview of the CSS structure and purpose.

---

## 4. Summary of Output

| Documentation | Output Directory |
|---------------|----------------|
| Javadoc       | build/docs/javadoc |
| JSDoc         | build/docs/jsdoc   |
| KSS           | build/docs/kss     |

---

## 5. Recommended Workflow

To generate all documentation at once, run the provided shell script:

```bash
./generate-docs.sh
```

This script will:

1. Generate Javadoc for all Java backend code.
2. Generate JSDoc for all JavaScript frontend files.
3. Generate KSS documentation for all CSS files.

After running, verify that the following directories exist:

- build/docs/javadoc
- build/docs/jsdoc
- build/docs/kss

---

## 6. Troubleshooting

- Malformed HTML in Javadoc: Replace < and > in emails or other HTML-like content with &lt; and &gt;.
- KSS homepage warning: Ensure homepage.md exists and path is correctly provided with --homepage.
- Deprecated KSS warnings: Can be ignored; they do not prevent documentation generation.
