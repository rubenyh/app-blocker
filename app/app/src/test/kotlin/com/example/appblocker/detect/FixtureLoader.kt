package com.example.appblocker.detect

internal fun loadYouTubeFixture(name: String): ScreenSnapshot {
  val lines = requireNotNull(ScreenSnapshot::class.java.getResource("/fixtures/youtube/$name.txt"))
    .readText().lines()
  val pkg = lines.first { it.startsWith("# app: ") }.removePrefix("# app: ").substringBefore(' ')
  val nodes = mutableListOf<SnapshotNode>()
  val ancestors = mutableListOf<Int>()
  val pattern = Regex("( *)([^|]+)\\|([^|]*)\\|sel=(true|false)\\|\\[(-?\\d+),(-?\\d+)\\]\\[(-?\\d+),(-?\\d+)\\]")
  for (line in lines.filter { it.isNotBlank() && !it.startsWith('#') }) {
    val fields = requireNotNull(pattern.matchEntire(line)) { "Invalid redacted fixture line" }.groupValues
    require(fields[1].length % 2 == 0)
    val depth = fields[1].length / 2
    require(depth <= ancestors.size)
    while (ancestors.size > depth) ancestors.removeAt(ancestors.lastIndex)
    val parent = ancestors.lastOrNull() ?: -1
    ancestors.add(nodes.size)
    nodes.add(SnapshotNode(
      fields[3].ifEmpty { null }, fields[2], fields[4].toBooleanStrict(),
      Bounds(fields[5].toInt(), fields[6].toInt(), fields[7].toInt(), fields[8].toInt()), parent,
    ))
  }
  return ScreenSnapshot(pkg, nodes)
}
