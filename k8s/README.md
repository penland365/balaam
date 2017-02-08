Kubernetes Configuration
====
If you have the `kubectl` CLI tool installed, deploying the Server is as simple as
```shell
$ kubectl create -f balaam-secrets.yaml
$ kubectl create -f balaam-svc.yaml
$ kubectl create -f balaam-deployment.yaml
```
To get the IP Address of the exposed service, you can run
```shell
$ kubectl get svc balaam
```
or, if you have [jq](https://stedolan.github.io/jq/) installed
```shell
kubectl get svc balaam -o json | jq '. | {ip: .status.loadBalancer.ingress[0].ip}'
```

The scripts are tuned for use on Google Cloud's Kubernetes' Service in that the instantiated service requests a `LoadBalancer` IP Address. Other than that the scripts are generic to any Kubernetes deployment.

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
