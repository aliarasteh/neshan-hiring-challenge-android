## **Data Module**

This is the core data module responsible for managing local and network repositories.



### **Architecture overview**

The clean architecture assumes separation of concerns with UI(Activity, Fragment, View), Presentation(ViewModel), and Data(Repository) layers. ViewModel helps to handle user interaction, save the current state of the app and automatically manage Android UI components lifecycle via LiveData.
The repository serves as a data point, where ViewModel knows nothing about the source of data. It is up to the repository to decide whether local or remote data should be given back to the user.
