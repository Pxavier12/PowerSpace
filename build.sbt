name := "Powerspace_Test"
version := "0.1"
scalaVersion := "2.13.8"

val http4sVersion = "0.23.18"
val circeVersion  = "0.14.5"
val grpcVersion   = "1.56.1"
val scalapbVersion = "0.11.13"

lazy val commonDependencies = Seq(
  "org.http4s"    %% "http4s-ember-server" % http4sVersion,
  "org.http4s"    %% "http4s-ember-client" % http4sVersion,
  "org.http4s"    %% "http4s-circe"        % http4sVersion,
  "org.http4s"    %% "http4s-dsl"          % http4sVersion,
  "io.circe"      %% "circe-generic"       % circeVersion,
  "io.circe"      %% "circe-literal"       % circeVersion,
  "ch.qos.logback" % "logback-classic"     % "1.4.7"
)

lazy val protobufDependencies = Seq(
  "com.thesamet.scalapb" %% "scalapb-runtime" % scalapbVersion,
  "io.grpc" % "grpc-netty" % grpcVersion,
  "io.grpc" % "grpc-protobuf" % grpcVersion,
  "io.grpc" % "grpc-stub" % grpcVersion,
  "io.grpc" % "grpc-services" % grpcVersion
)

lazy val bidder = (project in file("bidder"))
  .enablePlugins(Fs2Grpc)
  .settings(
    libraryDependencies ++= commonDependencies ++ protobufDependencies,
    Compile / PB.targets := Seq(
      scalapb.gen() -> (Compile / sourceManaged).value / "scalapb"
    )
  )

lazy val adserver = (project in file("adserver"))
  .settings(
    libraryDependencies ++= commonDependencies
  )
  .dependsOn(bidder)
