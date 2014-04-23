## 0.1.2

- Prepare project for releases with lein-pallet-release

- Update .gitignore with some generated files.

- Change license to EPL (was All Rights Reserved).

- Updated README with new way to launch via leiningen.

- Fix exception when destroying cluster after job termination.

- Remove code obfuscation config, and update jclouds deps.

- Update dependencies to public releases and latest versions.

- Remove expiration checks.

- More robust checking for when to use and create /etc/hosts (for VBox
  mainly).

- Update to latest dependencies

- Update to pallet-0.8.0-beta.1

- Add step number to job logging output

- Enable running cli tasks without exit
  In order to facilitate development and testing, allow disabling of exits
  from the CLI.

- Make use of /etc/hosts provider specific

- Update example bootstrap script to write a file

- Add very simple example bootstrap script

- Update .gitignore

- Add bricklayer test for dist.sh
  Add script to exercise dist.sh using bricklayer.

- Add script actions to jobs
  Fixes #2

- Update to pallet 0.8.0-alpha.8

- Update to latest versions

# Release Notes

## 0.1.1

- Update dist.sh to build in a subdirectory

- Add ReleaseNotes.md to the distribution

- Remove credentials from default logging

## 0.1.0

- Use ec2 credenttials as default credentials for s3
