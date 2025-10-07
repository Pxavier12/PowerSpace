package com.powerspace.test

import java.time.Instant

case class AdGroup(
  id: Int,
  name: String,
  description: String,
  image: String,
  countryCode: String,
  cost: Double,
  costType: String,
  budget: Double,
  start: Option[Instant],
  end: Option[Instant]
) {}
