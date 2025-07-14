package core

import zio.json.ast.Json
import zio.json.JsonDecoder

final case class WeatherInfoResponse(
    location: CurrentLocation,
    current: CurrentWeather
) derives JsonDecoder

final case class CurrentLocation(
    name: String,
    region: String,
    country: String,
    lat: Double,
    lon: Double,
    tz_id: String,
    localtime_epoch: Long,
    localtime: String
) derives JsonDecoder

final case class CurrentWeather(
    last_updated_epoch: Long,
    last_updated: String,
    temp_c: Double,
    temp_f: Double,
    is_day: Int,
    condition: WeatherCondition,
    wind_mph: Double,
    wind_kph: Double,
    wind_degree: Int,
    wind_dir: String,
    pressure_mb: Double,
    pressure_in: Double,
    precip_mm: Double,
    precip_in: Double,
    humidity: Int,
    cloud: Int,
    feelslike_c: Double,
    feelslike_f: Double
) derives JsonDecoder

final case class WeatherCondition(
    text: String,
    icon: String,
    code: Int
) derives JsonDecoder