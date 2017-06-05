#Configuring Social Login

This application supports integrating with Facebook login using Spring Security. Once enabled, updating movie information would be allowed only for the authenticated users.

Follow these instructions to enable Facebook login integration during the initial deployment.

1. Register a new Facebook app at <https://developers.facebook.com/>.
  * Make a note of its App ID and App Secret.
  * Enable Web OAuth Facebook login.
  * Provide a temporary Valid OAuth redirect URIs value, ie http://localhost:8080/. _You will need to update this later once you know the actual web app url after the deployment completes._
2. Edit [\deployment\config.json](config.json) to provide these values for the FACEBOOK_APP_ID and FACEBOOK_APP_SECRET environment variables.
3. Deploy using instructions provided in the [\deployment\readme.md](readme.md)
4. Update the Facebook app created in (1) with the actual value for the redirect URI.