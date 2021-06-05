package shared.engine

object Threading {
  def registerShutdownHook() = {
    Runtime
      .getRuntime()
      .addShutdownHook(
        new Thread() {
          override def run() = {
            Thread
              .getAllStackTraces()
              .keySet()
              .forEach(thread => thread.interrupt())
          }
        }
      )
  }
}
