raytracer-scala
===============
A simple raytracer, programmed in Scala. Uses Akka for parallelization and ScalaFX for the GUI. It basically is a Scala
rewrite of the program described in this [http://blogs.msdn.com/b/lukeh/archive/2007/04/03/a-ray-tracer-in-c-3-0.aspx](blog post).
I've added support for supersampling AA and distributing the rendering out to multiple actors. 

Building
==============
The project uses SBT. To run it, simply `git clone` this repository, navigate to it and `sbt run` to let SBT work its magic.
