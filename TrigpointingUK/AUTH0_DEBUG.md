# Auth0 Debug Information

## Current Configuration
- **Domain**: trigpointing.eu.auth0.com
- **Client ID**: IEBodjQvHMuDTS5vNVeve5j8YKQcYBN3
- **Redirect URI**: uk.trigpointing.android://trigpointing.eu.auth0.com/android/uk.trigpointing.android.debug/callback
- **Audience**: https://api.trigpointing.uk/api/v1/

## Auth0 Dashboard Checklist

### 1. Application Settings
- [ ] Application Type: **Native**
- [ ] Token Endpoint Authentication Method: **None** (for public clients)
- [ ] Application Status: **Active**

### 2. URLs Configuration
- [ ] Allowed Callback URLs:
  ```
  uk.trigpointing.android://trigpointing.eu.auth0.com/android/uk.trigpointing.android.debug/callback
  ```
- [ ] Allowed Logout URLs:
  ```
  uk.trigpointing.android://trigpointing.eu.auth0.com/android/uk.trigpointing.android.debug/callback
  ```
- [ ] Allowed Web Origins:
  ```
  https://api.trigpointing.uk
  ```

### 3. Advanced Settings
- [ ] Grant Types: **Authorization Code**, **Refresh Token**
- [ ] OIDC Conformant: **Yes**
- [ ] Refresh Token Rotation: **Yes** (recommended)

## Common Issues

### Issue 1: "Error Page" on Auth0
**Cause**: Application not properly configured or inactive
**Solution**: 
1. Check application is active in Auth0 Dashboard
2. Verify callback URL matches exactly
3. Ensure application type is "Native"

### Issue 2: Redirect URI Mismatch
**Cause**: Callback URL doesn't match Auth0 configuration
**Solution**: 
1. Copy the exact redirect URI from the error
2. Add it to Auth0 Dashboard "Allowed Callback URLs"
3. Note the `.debug` suffix in the URI

### Issue 3: Application Type Wrong
**Cause**: Application configured as "Single Page Application" instead of "Native"
**Solution**: Change application type to "Native" in Auth0 Dashboard

## Testing Steps

1. **Test Auth0 Universal Login directly**:
   - Go to: https://trigpointing.eu.auth0.com/authorize?client_id=IEBodjQvHMuDTS5vNVeve5j8YKQcYBN3&response_type=code&redirect_uri=uk.trigpointing.android://trigpointing.eu.auth0.com/android/uk.trigpointing.android.debug/callback&scope=openid%20profile%20email&audience=https://api.trigpointing.uk/api/v1/

2. **Check Auth0 Logs**:
   - Go to Auth0 Dashboard > Monitoring > Logs
   - Look for errors related to your client ID

3. **Verify Application Status**:
   - Go to Auth0 Dashboard > Applications
   - Check if your application is active and properly configured

## Next Steps

1. Fix any configuration issues in Auth0 Dashboard
2. Test the universal login URL directly in a browser
3. Try the Auth0 login again in the app
4. Check Logcat for detailed error messages

## Logcat Filtering

To see Auth0-related logs, filter by:
- `Auth0Config`
- `AuthPreferences` 
- `MainActivity`
- `Auth0`

Example: `adb logcat | grep -E "(Auth0Config|AuthPreferences|MainActivity|Auth0)"`
