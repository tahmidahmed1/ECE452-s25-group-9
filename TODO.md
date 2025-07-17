# TODO

> File to add any ideas someone has for the project while developing so tickets can be created for them.

- [ ] Fix splash screen and remove the default android splash screen
- [ ] When signing in, we see the signin page before the home page
- [ ] Enforce numbers for fields like phone number and age
- [ ] Add animations
- [ ] Fix custom skills so it actually makes a tag we can't see the skills added and add a max of 10
- [ ] Fix README
- [ ] Fix settings page to have all onboarding info edit
- [ ] Write test cases
- [ ] Make sure if a user does onboarding but closes the app or signs out then tries to sign in again, they are redirected to the onboarding page
- [ ] Modularize endpoints into separate files
- [ ] Remove comments and unused code from codebase
- [ ] Make sure location services is requested before displaying events
- [ ] Set up google maps api key so we can view events
- [ ] add subscription so volunteers can subscribe and get notified when new events are added
- [ ] Add banners for organizers
- [ ] Fix all warnings in project
- [ ] Set up user notifications and set up chat functionality
- [ ] Replace all TODO instances in codebase
- [ ] Go through the project proposal and make sure we follow the services we wrote
- [ ] remove endpoints in python not being used
- [ ] let's make sure volunteers earn karma only once the event date passes and the organizer has confirmed the volunteer's attendance
- [ ] add a subscription feature so volunteers can subscribe and get notified for events
- [ ] add something on organizer home screen since it's so empty
- [ ] allow organizers to approve hours for volunteers cuz after the event volunteers will submit a request and the number of hours and descriptioon on what they did and organizations should approve it
- [ ] fix readme and make it look nice
- [ ] make sure images load
- [ ] fix the radius circle and zoom cuz the circle does not fit in the screen
- [ ] add filters for volunteer events
let's make a todo list. in our ListScreen we have a radius dropdown but sometimes it dissapears randomly. We should remove this and make it a slider as part of the filters amd default to 50km. Another thing to implement is the search bar should search for actual organizations. Each of these tiles should be clickable so it goes to the organization profile where it will have a subscribe button where a user can subscribe to the organization and get notified for new events and a message button where they can message the organization. When clicking message it will show a popup to send an initial message which starts the chat. Right now the chat screen is mocked so I want to implement full functionality as well as in the backend. Let's also implement backend functionality for subscribing to organizers. In the stats screen there is a mocked list of organizers we have subscribed to which should be implemented and have an unsubscribe button. It should have full functionality and show a toast plan this to do list carefully and understand what we want to do. make sure to update our backend and every few changes make sure we run ./gradlew assembleDebug to see any issues.
