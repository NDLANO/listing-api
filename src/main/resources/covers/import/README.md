### Importing covers to the database
Use the script "import_covers" in the deploy repo. 

``` $ ndla nodes import_covers [env] -f [name of csv file] -T [name of theme]```

The theme must be set in ```no.ndla.listingapi.model.meta.Theme.allowedThemes``` 

Add/update the csv files to this folder so that a complete re-import can ble possible. 