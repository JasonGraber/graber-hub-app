# Security Policy

The app is designed to connect to Graber Hub over a trusted home network or private VPN. Cleartext HTTP is permitted because local Raspberry Pi installs commonly do not have TLS.

- Do not point the app at an untrusted server.
- Do not expose the Hub API directly to the public Internet.
- Keep `local.properties`, private IPs, Tailscale addresses, signing keys, and family data out of Git.
- Report vulnerabilities through a private GitHub security advisory.

Security fixes target the latest `main` branch.
