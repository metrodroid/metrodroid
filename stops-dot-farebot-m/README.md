# stops-dot-farebot-m

We used to collect Stop ID issues with an App Engine application. Due to a lack of use, this application has now been turned down, and replaced with a static page directing visitors to GitHub's issue tracker.

If there is utility in this in future, this may be turned up again. See the git history for this repository to access the old code.

## Deploying to production

```
gcloud app deploy --project=farebot-m ./app.yaml
```

This will deploy the application to the `farebot-m` application, `stops` module, `1` version.  This is the production environment used by the developers.

It's unlikely that as a third party you'll have access to administer `farebot-m.appspot.com`, in which case you will need to work with your own App Engine project.  You can specify additional arguments in order to point to your version:

```
gcloud app deploy --project=example -M my-module -V 1 .
```

This will deploy to the `example` application, `my-module` module, `1` version.

If you're forking this application for your own use, you should consider editing `app.yaml` to set these attributes permanently.
