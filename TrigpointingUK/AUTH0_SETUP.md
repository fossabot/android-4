# Auth0 Integration Setup Guide

This guide explains how to set up Auth0 authentication for the TrigpointingUK Android app.

## Overview

The app now includes Auth0 authentication capabilities alongside the existing legacy authentication system. This allows for a smooth migration path where users can test Auth0 functionality while maintaining backward compatibility.

## Features Added

### Developer Menu Items (only visible when Developer Mode is enabled)

1. **Auth0 Login** - Uses Auth0 universal login to authenticate users
2. **Auth0 User** - Displays detailed debug information about Auth0 tokens and user profile
3. **Auth0 API** - Makes an API call to `/user/me` endpoint using Auth0 access token
4. **Auth0 Logout** - Logs out from Auth0

### Database Schema Changes

The `User` class has been extended with two new fields:
- `auth0_user_id` (String) - Stores the Auth0 user ID
- `auth0_username` (String) - Stores the sanitized username for Auth0

## Setup Instructions

### 1. Auth0 Dashboard Configuration

1. **Create a Native Application** in your Auth0 dashboard
2. **Configure the application** with these settings:
   - **Application Type**: Native
   - **Token Endpoint Authentication Method**: None (for public clients)
   - **Allowed Callback URLs**: `uk.trigpointing.android://your-tenant.auth0.com/android/uk.trigpointing.android/callback`
   - **Allowed Logout URLs**: `uk.trigpointing.android://your-tenant.auth0.com/android/uk.trigpointing.android/callback`
   - **Allowed Web Origins**: `https://api.trigpointing.uk`

### 2. Update Configuration Files

Edit `/app/src/main/res/values/auth0_config.xml` and replace the placeholder values:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="auth0_domain">your-actual-tenant.auth0.com</string>
    <string name="auth0_client_id">your_actual_client_id</string>
    <string name="auth0_redirect_uri">uk.trigpointing.android://your-actual-tenant.auth0.com/android/uk.trigpointing.android/callback</string>
    <string name="auth0_audience">https://api.trigpointing.uk/api/v1</string>
</resources>
```

### 3. API Configuration

Ensure your FastAPI backend is configured to:
- Accept Auth0 access tokens in the `Authorization: Bearer <token>` header
- Validate tokens using your Auth0 domain and audience
- Return user information at the `/user/me` endpoint

### 4. Database Migration

Add the new columns to your legacy user table:

```sql
ALTER TABLE users ADD COLUMN auth0_user_id VARCHAR(24);
ALTER TABLE users ADD COLUMN auth0_username VARCHAR(255);
```

## Usage

### For Developers

1. **Enable Developer Mode** in the app settings
2. **Access Auth0 Menu Items** from the main menu (only visible in developer mode)
3. **Test Auth0 Login** - Use the "Auth0 Login" menu item to test authentication
4. **Debug Tokens** - Use "Auth0 User" to inspect tokens and user profile data
5. **Test API Calls** - Use "Auth0 API" to test backend integration
6. **Test Logout** - Use "Auth0 Logout" to test logout functionality

### For Migration

The system is designed to support both authentication methods during migration:

1. **Legacy users** can continue using the existing login system
2. **New users** can be directed to use Auth0
3. **Migration errors** are logged to both Logcat and Firebase Crashlytics
4. **Fallback handling** redirects Auth0 "user not found" errors to legacy login

## Error Handling

All Auth0 operations include comprehensive error handling:

- **Network errors** are logged and reported to Crashlytics
- **Authentication failures** are logged with detailed error messages
- **Token validation errors** are handled gracefully
- **Migration issues** are logged for debugging data quality problems

## Logging

Auth0 operations are logged with the following tags for easy filtering:
- `Auth0Config` - Auth0 configuration and setup
- `AuthPreferences` - Token storage and retrieval
- `MainActivity` - UI interactions and error handling

## Security Considerations

- **Tokens are stored securely** in Android SharedPreferences
- **Token expiration** is checked before use
- **Logout clears all tokens** from local storage
- **Network calls use HTTPS** for all API communication

## Troubleshooting

### Common Issues

1. **"Not logged in with Auth0"** - User needs to complete Auth0 login first
2. **"No Auth0 access token available"** - Token may have expired, try logging in again
3. **API call failures** - Check that the backend is properly configured to accept Auth0 tokens
4. **Login failures** - Verify Auth0 configuration values are correct

### Debug Steps

1. Check Logcat for detailed error messages
2. Use the "Auth0 User" menu item to inspect token status
3. Verify Auth0 dashboard configuration matches app settings
4. Test API endpoint directly with a valid Auth0 token

## Future Migration Steps

Once the legacy database is cleaned and all users are migrated:

1. **Remove legacy authentication** code
2. **Update user interface** to only show Auth0 login
3. **Remove developer menu items** or make them production features
4. **Clean up database** by removing legacy authentication fields

## Support

For issues related to:
- **Auth0 configuration** - Check Auth0 dashboard settings
- **API integration** - Verify backend token validation
- **App functionality** - Check Logcat and Crashlytics logs
- **Database issues** - Review migration logs for data quality problems
