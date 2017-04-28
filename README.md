# listing-api
[![Build Status](https://travis-ci.org/NDLANO/listing-api.svg?branch=master)](https://travis-ci.org/NDLANO/listing-api)

## Usage

API for NDLAs "show and filter" service for e.g. an overview of articles.
Creates, updates and returns a ```cover```, a short summary representation of an article's metadata. Is currently only used by the frontend [Utlisting](https://listing-frontend.test.api.ndla.no/listing/betongfaget), which _only_ gets and displays the data. It is possible to add new or update covers through the use of this API, as of now this is not implementet by any service except 
the original/one-off data import script.


To interact with the api, you need valid security credentials; see [Access Tokens usage](https://github.com/NDLANO/auth/blob/master/README.md).
To write data to the api, you need write role access.

For a more detailed documentation of the API, please refer to the api documentation for the relevant environment: 
* [API documentation stageing](https://staging.api.ndla.no)
* [API documentation production](https://api.ndla.no)

## Developer documentation

**Compile:** sbt compile

**Run tests:** sbt test

**Create Docker Image:**./build.sh