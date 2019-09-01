# Purpose

Card images serve as an indicator of supported cards. It needs to be
present on both iOS and Android but the system have different opinion on
how to handle images. So iOS images are auto-generated.

# Image placement

* Raster images (png and jpeg) go to `cardimages/android/drawable-hdpi`
* For vector images 2 copies need to be present: an svg one in `cardimages/svg`
  and `xml`one in `cardimages/android/drawable`

# Generating

To generate you need inkscape available in your PATH. Hence it's not done
as part of standard build process. After adding or modifying images run

```shell

./gradlew :iOSImages

```
