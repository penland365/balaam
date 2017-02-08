Kubernetes Configuration
====

## balaam-secrets
To ensure you don't accidentally enter `balaam-secrets.yaml` into source control, that file has been specifically added to a `.gitignore` file at the root of this folder.

An example `balaam-secrets.yaml` file is shown below
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: balaam-secrets
type: Opaque
data:
  google.api.key:
  darksky.api.key:
```
Please note that any secret added to this file should be base64 encoded prior to adding it to the file.
