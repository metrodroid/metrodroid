# stops-dot-farebot-m

This is a Google App Engine application for collecting information about unknown stops.

This is designed to be fairly generic in nature, so it can be extended into other transit agencies.

This can run inside the App Engine free tier, or with $0 budget on low traffic installations.  This does not use any billing-restricted APIs.

## Setting up the Cloud SDK

Install [gcloud](https://cloud.google.com/sdk/gcloud/), and make sure it is in your `PATH`.  Then set up the App Engine SDK with:

```
gcloud components install app-engine-python
```

Note: You must use the App Engine SDK bundled with `gcloud`.  The tooling does not support the standalone version of the App Engine SDK.

## Configuring `secrets.yaml`

You'll need to create a secrets.yaml file to begin.  It should look something like this:

```yaml
env_variables:
  SECRET_KEY: your-secret-key-goes-here
```

You can generate a secret key with some short Python code, eg:

```python
import os
os.urandom(33).encode('base64')
```

Do not commit your `secrets.yaml` to the repository, as this is used for the session management in the application, which has some security functions.

## Running the development server

```
./dev_appserver.sh
```

This will automatically locate your installation of the App Engine SDK (provided it is installed via `gcloud`), and run the development server.  The application will be accessible on http://localhost:8080/

## Deploying to production

```
./appcfg.sh update -V 1 .
```

This will deploy the application to the `farebot-m` application, `stops` module, `1` version.  This is the production environment used by the developers.

It's unlikely that as a third party you'll have access to administer `farebot-m.appspot.com`, in which case you will need to work with your own App Engine project.  You can specify additional arguments in order to point to your version:

```
./appcfg.sh update -A example -M my-module -V 1 .
```

This will deploy to the `example` application, `my-module` module, `1` version.

If you're forking this application for your own use, you should consider editing `app.yaml` to set these attributes permanently.

## Configuring entities

### Setting up an Agency

All entities belong to an Agency.  This represents an organisation who are responsible for the ticketing system.  You will need to create an Agency for each card for which you are collecting data for.

When starting out, you'll need to use the Interactive Console (http://localhost:8000/console) to create a new Agency.

```python
import models
models.Agency(name='Example Transit Smartcard', location='Norfolk Island', vehicle_types=['bus']).put()
```

Supported vehicle types are listed in `models.py`.

In production, you will need to use the Datastore Admin in the Developers Console (https://console.developers.google.com).

Because repeating fields are not supported in the Datastore Admin, you will need to first create the agency, then [locate it in the list](http://localhost:8080/app/).  Get it's ID, then change visit the vehicle type editor page:

```
http://localhost:8080/app/edit/agency/123123123123123/
```

Where `123123123123123` is the agency ID.  This will let you edit the vehicle types for the agency.

Note: you will get a blank page if you are not logged in as an Admin when attempting to use the vehicle type editor page.

### Setting up stops

Setting up stops is optional.  This is useful for ferries and rail where there are a small number of stations in fixed locations.  When users provide information about an unknown stop and there is a list of stops available, and they may optionally select the stop from a drop-down list.

You can use the Datastore Admin to set up stops.  However, this may not be convienient if you have a large amount of stops to enter.

Instead, you can use the stops import tool:

```
http://localhost:8080/app/edit/agency/123123123123123/stops_import
```

Where `123123123123123` is the agency ID.  This will let you add a large number of stops to an agency from a CSV file.  Follow the instructions on the page for the headers expected.

All files must be in "excel CSV" format, that is, comma separated with optional double quotation marks `"` surrounding the field.

`y` and `x` are the latitude and longitude of the stop, respectively, in the [WGS84 geodetic datum](https://en.wikipedia.org/wiki/World_Geodetic_System).  If you don't know what "geodetic datum" means, this is the "normal" latitude and longitude of the stop, in decimal degrees, with north and east being positive, and south and west being negative.

At the moment the latitude and longitude points are not yet used by the application.  It is planned for this to be made available as part of a stop selection in future, where it would be particularly useful for bus stops.


