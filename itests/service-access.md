
## Azure

## Box

## Braintree

## Geolocation

## Github

## Google API

https://console.developers.google.com

1) Create a project
2) Create an OAuth consent screen
3) Create OAuth Client credentials (OAuth for WebApp)
4) Add `https://developers.google.com/oauthplayground` to Authorized redirect URIs
5) Setup `https://developers.google.com/oauthplayground` using credentials from above
6) Authorize APIs for
   Gmail API v1 scope `https://mail.google.com`
   Calendar API v3 scope `https://www.googleapis.com/auth/calendar`
   Drive API v3 scope `https://www.googleapis.com/auth/drive`
7) Exchange authorization code for tokens
8) Note down Access token & Refresh token
9) Enable the API and Services
   GMail `https://console.developers.google.com/apis/library/gmail.googleapis.com`
   Drive `https://console.developers.google.com/apis/library/drive.googleapis.com`
   Calendar `https://console.developers.google.com/apis/library/calendar-json.googleapis.com`

export GOOGLE_API_APPLICATION_NAME="Fuse WFC Testsuite"
export GOOGLE_API_CLIENT_ID=101xxx.apps.googleusercontent.com
export GOOGLE_API_CLIENT_SECRET=xxx
export GOOGLE_API_REFRESH_TOKEN=xxx

## Openweather

## Salesforce

https://developer.salesforce.com
https://eu13.lightning.force.com/lightning/setup

https://help.salesforce.com/articleView?id=connected_app_create_api_integration.htm
https://help.salesforce.com/articleView?id=remoteaccess_oauth_username_password_flow.htm

   Note, when using the username-password flow with an API, create a field in the username and password login screen where users can enter 
   their security token. The security token is an automatically generated key that must be added to the end of the password to log in to 
   Salesforce from an untrusted network. Concatenate the password and token when passing the request for authentication.
   
Note, I found that spcial chars in the password (i.e. $ @) may be cause authorization to fail.
Other than documented above, the security code does not need to be appended to password.

export SALESFORCE_CONSUMER_KEY=3MVG9xxx
export SALESFORCE_CONSUMER_SECRET=772FExxx
export SALESFORCE_USER=fuse.wfc.ci@gmail.com
export SALESFORCE_PASSWORD=xxx

curl \
--data "client_id=$SALESFORCE_CONSUMER_KEY" \
--data "client_secret=$SALESFORCE_CONSUMER_SECRET" \
--data "username=$SALESFORCE_USER" \
--data "password=$SALESFORCE_PASSWORD" \
--data "grant_type=password" \
https://login.salesforce.com/services/oauth2/token

## Servicenow

## Twilio
