# Weather Radar [![Build Status](https://travis-ci.org/dh4/WeatherRadar.svg?branch=master)](https://travis-ci.org/dh4/WeatherRadar)

<p align="center"><img src="https://raw.githubusercontent.com/dh4/WeatherRadar/master/resources/icon_128.png" alt="Weather Radar" /></p>

Weather Radar is an open-source application featuring doppler radar images from the United States National Weather Service and Weather Underground's API.

The images are single images based on location rather than tiled images that can be panned like most other applications of this type.
This allows faster loading when connection speeds are sub-optimal.
Images can be customized including by resolution to help keep data usage to a minimum when needed.
You can save your favorite views for easy reference.

National Weather Service radar imagery is available for the United States and its territories.
The Weather Underground radar imagery requires an API key from their website to work.
There is a test feature available so you can try it out before acquiring your own API key.
Their radar imagery is available for the United States and some parts of Canada, Mexico, Western Europe, and Australia.
Their satellite imagery is available worldwide.

__NOTE:__ Weather Underground discontinued free API keys at the same time I released this application.
If you already have an API key from them, it will work.
Otherwise, there doesn't seem to be a way to get the Weather Underground images working.
The NWS images still work fine.

I will work on integrating another source.

## Downloading

<a href="https://play.google.com/store/apps/details?id=com.danhasting.radar">
    <img src="https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png"
         alt="Get it on Google Play" height="80">
</a>
<a href="https://f-droid.org/packages/com.danhasting.radar/">
    <img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
         alt="Get it on F-Droid" height="80">
</a>



APKs are also available on the [releases](https://github.com/dh4/WeatherRadar/releases) page.

## Screenshots

<p align="center">
<img src="https://raw.githubusercontent.com/dh4/WeatherRadar/master/metadata/en-US/images/phoneScreenshots/1.png" alt="Screen Shot" width="250" />
<img src="https://raw.githubusercontent.com/dh4/WeatherRadar/master/metadata/en-US/images/phoneScreenshots/2.png" alt="Screen Shot" width="250" />
<img src="https://raw.githubusercontent.com/dh4/WeatherRadar/master/metadata/en-US/images/phoneScreenshots/3.png" alt="Screen Shot" width="250" />
</p>

## Support

If you find it useful, please consider supporting my work here:
https://dh4.github.io/donate/

## Development Builds

An automatic debug build of the latest git commit can be found here. This can be installed alongside the release version:
https://s3.amazonaws.com/dh4/weather_radar/master/com.danhasting.radar.devel.apk
