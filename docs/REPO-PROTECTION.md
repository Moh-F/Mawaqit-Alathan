# Repository protection (owner checklist)

Done in files: LICENSE (all rights reserved), SECURITY.md, CODEOWNERS, PR template, copyright headers in source, release workflow limited to the owner's repository.

Do once in GitHub (Settings of the repo):
1. **Rules → Rulesets → New ruleset → Import a ruleset**: import `ruleset-main.json` and `ruleset-tags.json` from this folder (protects `main`, protects release tags `v*`).
2. **Code security**: enable Dependabot alerts, Secret scanning and **Push protection**, and **Private vulnerability reporting**.
3. **Actions → General**: allow GitHub + verified-creator actions only; "Require approval for all outside collaborators"; Workflow permissions = **Read repository contents** (not read/write).
4. **General**: turn off Wiki, Projects and Discussions if you do not use them.
5. **Account**: two-factor authentication with a passkey or authenticator app; review Settings → Applications for unknown tokens.
6. Keep the signing key and its password offline in two places; rotate GitHub secrets if a device is lost.

Known limits: anyone can view and fork a public repo (GitHub terms), and the public APK can be decompiled. The license and protections prevent unauthorized changes to YOUR repository and make misuse a rights violation, but cannot technically stop copying of public files.
