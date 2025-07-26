# Navigation Architecture

This directory contains the navigation components for the Good Deed Feed app, implementing a role-based tab navigation system.

## Overview

The navigation system dynamically shows different tab configurations based on the user's role, providing a tailored experience for each user type.

## User Types and Navigation

### 1. Volunteers (`UserType.VOLUNTEER`)
**Tab Configuration:** 4 tabs
- **Home** - Dashboard and main content
- **Events** - List view of available volunteer opportunities and organizer subscriptions
- **Map** - Map view showing volunteer opportunities geographically
- **Leaderboard** - User statistics, karma points, and rankings

### 2. Organizations (`UserType.ORGANIZER`)
**Tab Configuration:** 4 tabs
- **Home** - Dashboard and main content
- **Events** - CRUD operations for managing volunteer events
- **Search** - Search and discovery functionality
- **Leaderboard** - User statistics, karma points, and rankings

## File Structure

```
navigation/
├── README.md                    # This documentation file
├── AppNavHost.kt               # Main navigation host with route definitions
├── TabNavigation.kt            # Tab navigation wrapper component
├── NavigationConfig.kt         # Centralized tab configuration for all user types
└── Screen.kt                   # Screen route definitions (if exists)
```

## Key Components

### `NavigationConfig.kt`
Central configuration object that defines all tab configurations:
- `getVolunteerTabs()` - Returns tabs for volunteer users
- `getOrganizerTabs()` - Returns tabs for organization users
- `getDefaultTabs()` - Returns fallback tabs
- `getTabsForUserType()` - Main entry point for tab selection

### `TabNavigation.kt`
Main tab navigation component that:
- Receives user data and determines appropriate tabs
- Renders bottom navigation bar
- Handles tab selection and screen switching

### `AppNavHost.kt`
Top-level navigation handling:
- Authentication flow
- Route to tab navigation after successful login
- Handles navigation between major app sections

## Usage

The navigation system automatically adapts based on the user's type. When a user signs in:

1. The `AppNavHost` determines if the user is authenticated
2. If authenticated, it navigates to `TabNavigationScreen`
3. `TabNavigationScreen` calls `NavigationConfig.getTabsForUserType(user.user_type)`
4. The appropriate tab configuration is loaded and displayed

## Adding New Screens

To add a new screen to any user type:

1. Create the screen component in the `screens` directory
2. Add the screen to the appropriate tab configuration in `NavigationConfig.kt`
3. Update the imports in `NavigationConfig.kt`

Example:
```kotlin
TabItem(
    title = "New Screen",
    icon = Icons.Default.YourIcon,
    screen = { user, onLogout -> YourNewScreen(user = user) },
)
```

## Modular Architecture Benefits

- **Separation of Concerns**: Each user type has its own clearly defined navigation
- **Maintainability**: Easy to modify tabs for specific user types without affecting others
- **Scalability**: Simple to add new user types or modify existing ones
- **Type Safety**: Compile-time checking of tab configurations
- **Centralized Configuration**: All navigation logic is in one place

## Extension Functions

The `NavigationConfig.kt` file also includes useful extension functions:
- `UserType?.getDisplayName()` - Human-readable name for user types
- `UserType?.getTabCount()` - Number of tabs for each user type

These help with debugging and analytics. 
