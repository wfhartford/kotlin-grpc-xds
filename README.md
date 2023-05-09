# kotlin-grpc-xds

A sample project implementing two services in Kotlin; one client and one server. These two services communicate using gRPC, and connect to an istio service mesh using xDS.

These links were helpful getting everything going:

* [gRPC Load Balancing](https://grpc.io/blog/grpc-load-balancing/)
* [gRPC Proxyless Service Mesh](https://istio.io/latest/blog/2021/proxyless-grpc/)
* [Announcing Kotlin support for protocol buffers](https://developers.googleblog.com/2021/11/announcing-kotlin-support-for-protocol.html)
* [gRPC Kotlin](https://grpc.io/docs/languages/kotlin/)
* [grpc-kotlin examples](https://github.com/grpc/grpc-kotlin/tree/master/examples)
* [grpc-java xDS example](https://github.com/grpc/grpc-java/tree/master/examples/example-xds)
* [Istio Install](https://istio.io/latest/docs/setup/install/istioctl/)

## Deploying

This project can be deployed in two different ways: using images tagged with `latest`, or using recently built images by digest.

### Deploying pre-build images

If you want to deploy the services without building the project, use the `latest` tagged images by running:

```shell
kubectl apply -k kustomize/latest
```

### Building and deploying custom images

Building and deploying your own custom images is also possible. The gradle build uses the `image.name.prefix` property to name images, so modifying it in `gradle.properties` or specifying it when you build allows you to use whichever docker repo you like. Build the project by running:

```shell
./gradlew prepareDeployment -Pimage.name.prefix=my.docker.repo.com/user/kotlin-grpc-xds/
```

Then deploy the services using those images by running:

```shell
kubectl apply -k kustomize/digest
```

This deployment method uses image digests, so changes to source can easily be deployed by running the build and deploy commands again.

## Service Mesh

A service mesh such as Istio is a service which gives its user control over the network layer of a cluster. They typically implement a variety of useful features, check out [Istio's website](https://istio.io/) for details.

### Load Balancing

The advantage of a service mesh that this project is trying to demonstrate is load balancing. When a gRPC client interacts with a gRPC service, they normally make a single long-lived connection which may be used for many individual requests. This isn't a great situation because it means that one client is tied to one server for a relatively long time. This can lead to load imbalance where a small number of replicas could be heavily loaded while several others sit idle. The service mesh resolves this by load balancing by request rather than by connection.

### Proxy Load Balancing

Under normal circumstances, the service mesh accomplishes this load balancing by acting as a proxy between the client and the server. The proxy applies whatever networking rules the service mesh is configured for and establishes connections to the various server instances, distributing requests around the server cluster. This is a great solution, but introduces an extra network hop and the associated latency.

### Client Side Load Balancing

The Java gRPC implementation includes support for xDS, which is the protocol that the service mesh uses to communicate. By directly connecting to the service mesh control plane, the gRPC client is able to implement the load balancing logic on its own, eliminating the need for a proxy.

This blog post does a great job of explaining the difference and measuring the performance impact of proxy vs client-side load balancing: [gRPC Proxyless Service Mesh](https://istio.io/latest/blog/2021/proxyless-grpc/).

## Kustomize

In order to allow deployment using either `latest` tagged images, or images by digest, the kubernetes manifests and kustomization file layout is a little strange in this project.

### Manifest Layout

Here's the manifest layout I ended up with:

| Path                                                                                          | Description                                                                                                                                                                                                                                                                                                                                                                    |
|-----------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `/kustomize/latest/kustomization.yaml`                                                        | Main kustomization file used to deploy service using the `latest` image tag. Run `kubectl apply -k kustomize/latest` from the project root dir to deploy services using the default image name and `latest` tag. Deploying like this does not require building the project.                                                                                                    |
| `/kustomize/digest/kustomization.yaml`                                                        | Main kustomization file used to deploy service using a specific digest of an image. Run `kubectl apply -k kustomize/latest` from the project root dir to deploy service using the recently built image digest. Deploying this way requires recent build artifacts, specifically `/client/build/kustomize/kustomization.yaml` and `/server/build/kustomize/kustomization.yaml`. |
| `/manifests`                                                                                  | Contains the default manifests as deployed by `/kustomize/latest/kustomization.yaml`. Deploying all of the non-kustomization manifests under this directory is equivalent to deploying using the _latest_ kustomize directory.                                                                                                                                                 |
| `/client/build/kustomize/kustomization.yaml` and `/server/build/kustomize/kustomization.yaml` | Kustomization files generated by the gradle build. These include the image transformer which applies the image hash and reference the manifests in `/manifests/client` and `/manifests/server` respectively. The image transformers in these `kustomization.yaml` files modify the client and server deployments to use the most recently built images by digest.              |
