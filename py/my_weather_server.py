import httpx
import logging
import sys
from datetime import datetime

from fastmcp import FastMCP

# Route logs to stderr so they do not corrupt the JSON communication pipe
logging.basicConfig(level=logging.INFO)

app = FastMCP("Global Weather Assistant")

# Corrected API Subdomains
GEOCODING_URL = "https://geocoding-api.open-meteo.com/v1/search"
WEATHER_URL = "https://api.open-meteo.com/v1/forecast"

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] (SERVER) [%(filename)s:%(lineno)d] %(message)s",
    handlers=[
        logging.StreamHandler(sys.stderr)  # <-- CRITICAL: Keeps stdout 100% pure JSON
    ]
)
logger = logging.getLogger(__name__)

@app.tool()
async def get_weather(city: str) -> str:
    """
    Fetches the live current weather data for any global city by name.

    Args:
        city: The name of the city (e.g., 'Tokyo', 'London', 'Paris').
    """
    if isinstance(city, dict):
        city = city.get("city", "Tokyo")

    logger.info("-" * 100)
    logger.info("calling get_weather")
    logger.info("-" * 100)


    async with httpx.AsyncClient() as client:
        geo_params = {"name": city, "count": 1, "language": "en", "format": "json"}
        try:
            logger.info(f"geo_params:{geo_params}")

            # Step 1: Geocode text name to GPS coordinates
            logger.info(f"calling {GEOCODING_URL}")
            geo_response = await client.get(GEOCODING_URL, params=geo_params)
            geo_response.raise_for_status()
            geo_data = geo_response.json()
            logger.info(f"geo_response:{geo_response}")

            if not geo_data.get("results") or len(geo_data["results"]) == 0:
                return f"Could not find coordinates or locate a city named '{city}'."

            # --- THE CRITICAL FIX: Target item 0 explicitly ---
            location = geo_data["results"][0]
            lat = location["latitude"]
            lon = location["longitude"]

            country = location.get("country", "Unknown Country")
            city_proper = location.get("name", city)

            # Step 2: Fetch current metrics using coordinates numbers
            weather_params = {
                "latitude": lat,
                "longitude": lon,
                "current_weather": True,
                "temperature_unit": "celsius"
            }
            logger.info(f"calling {WEATHER_URL}")
            weather_response = await client.get(WEATHER_URL, params=weather_params)
            logger.info(f"weather_response:\n{weather_response}")
            weather_response.raise_for_status()
            weather_data = weather_response.json()
            logger.info(f"weather_data: {weather_data}")
            current = weather_data.get("current_weather")
            if not current:
                return f"Failed to retrieve structured weather data for {city_proper}."
            #logger.info(f"current weather: {current}")
            temp = current.get("temperature")
            return f"Current {datetime.now()} weather in {city_proper}, {country}: {temp}°C. GPS: ({lat}, {lon})"

        except Exception as e:
            logger.exception(e)
            return f"An error occurred while contacting the weather service: {str(e)}"

if __name__ == "__main__":
    app.run()
