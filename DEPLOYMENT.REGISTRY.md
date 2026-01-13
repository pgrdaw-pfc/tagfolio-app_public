# Deployment Registry

## Version 1.14.6
* refactor(deploy): make deploy.sh more robust, add .env.example, update DEPLOYMENT.md, README.md and Memoria

## Version 1.14.5
* update(filter): default filter names also generated from comparators and dates
* refactor(gallery): dates shown in yyyy-mm-dd hh:mm:ss 24h format in metadata sidebar
* update(prod): app.base-url=https://tf.juangimenoarquitecto.com for shared filters and reports
* refactor(gallery): dates shown in yyyy-mm-dd hh:mm:ss 24h format

## Version 1.14.4
* feat(deploy): add README.md
* refactor(deploy): in TEST, check $USER is in docker group and add if necessary
* refactor(deploy): in TEST, instructions to install git on host machine

## Version 1.14.3
* refactor(deploy): update DEPLOYMENT.md
* refactor(deploy): in TEST, let hibernate update database with spring.jpa.hibernate.ddl-auto=update
* refactor(deploy): in TEST, check $USER is in docker group and add if necessary
* refactor(deploy): in TEST, check curl and docker are installed
* refactor(deploy): in TEST, compile the Java application inside the container so host does not need to install java

## Version 1.14.2
* refactor(deploy): in TEST, pull latest commits regardless branch
* refactor(gallery): increased batch size to improve ux
* refactor(alerts): opacity of container and darker text

## Version 1.14.1
* fix(database): Update Database identifies wrong metadata and corrects it
* fix(upload): resize image, store it in /originals and then read metadata and store it in database
* feat(database): Update database handles duplicated records in images
* feat(thumbnails): add 'Update Thumbnails' button to File
* fix(thumbnails): ensure thumbnails do not exceed thumbnail.max-dimension

## Version 1.14.0
* feat(memoria): Tagfolio-memoria.pdf
* feat(help): tags.html, filters.html and reports.html help pages
* feat(help): linked index in each help page
* feat(help): images.html
* feat(help): basic help pages
* feat(help): help images
* feat(help): nav-bar help menu
* feat(deploy): deployment scripts prompt for ssh key

## Version 1.13.0
* refactor(controller): delete unused controllers
* fix(documentation): script and instructions for documentation
* fix(documentation): kss documentation for all .css files
* fix(tag-input-new): search and select functionality
* refactor(css): print.css, duplicated code and id selectors
* feat(documentation): kss documentation for all .css files
* feat(jsdoc): installed jsdoc to generate .js documentation
* fix(documentation): replace all <pgrdaw@gmail.com> by &lt;pgrdaw@gmail.com&gt;
* refactor(dropdown): dropdown.js to group code used in navbar.js and users-list.js
* feat(documentation): jsdoc documentation for all .js files
* refactor(user): user fetching in Controllers to consistently use userService.getCurrentUser()
* feat(documentation): memoria.pdf
* feat(documentation): javadoc documentation for .java files
* feat(images): previous/next button  navigate according to the user's current context in the gallery
* refactor(gradle): remove guava dependency
* refactor(gradle): remove metadata-extractor dependency

## Version 1.12.0
* fix(filter): autocomplete-dropdown visible overflowing
* feat(filter): place new-filter-input inside filter-expression-container
* fix(anonymous): access to images in shared/reports
* feat(anonymous): gallery button redirects to /shared/filter/{token}
* feat(anonymous): show warning when trying to access not allowed resources
* feat(anonymous): able to see detail of images in a shared filter
* feat(admin): able to see all images, tags, reports and filters from all users

## Version 1.11.2
* refactor(backend): remove unnecessary comments, unused imports, duplicated code
* refactor(image): remove unnecessary comments
* refactor(image): remove getTagNames() and extract names from getTags() when needed
* refactor(image): move getTitle() from Image.java to PublicImageController.java
* refactor(image): remove unused methods

## Version 1.11.1
* refactor(reports): remove unnecessary comments
* refactor(reports): adjust extended report rendering or pdf
* refactor(reports): adjust compact report rendering or pdf
* feat(deploy): deploy always main branch
* feat(share): shared url depends on environment dev/test/prod
* feat(upload): extract and store original Created and Modified metadata

## Version 1.11.0
* fix(users): nav-bar in user management view
* feat(users): gallery button
* feat(users): default user role
* fix(users): reuse available css

## Version 1.10.0
* feat(aws): aws deployment

## Version 1.9.1
* feat(error): unify all errors in error.html
* feat(responsive): seeding.html welcome.html error.html
* feat(responsive): dropdown-menu-content
* feat(responsive): responsive login and register templates
* feat(images): gallery, previous, next buttons
* feat(images): one column metadata display for mobile layout
* feat(images): two column metadata display
* feat(images): full width image  /images/{id}
* feat(images): hide sidebars in  /images/{id}
* fix(images): serve correct storage/originals/user_id/image_filename.jpf to /images/{id}
* fix(metadata): image.exiftool.display-metadata-keys

## Version 1.9.0
* fix(tests): testDeleteImage testCreateImage
* feat(database): spinner while re-sync database
* feat(database): add update-database-btn to re-sync database
* feat(storage): images stored in a different directory for each user
* feat(filters): add-selected-tags-btn

## Version 1.8.1
* feat(filters): add-selected-tags-btn
* feat(responsive): update selection.js for mobile layout
* feat(responsive): contents-sidebar paddings and border-radius
* feat(responsive): sidebar__section-input list in descending id number
* feat(responsive): sidebar__section-input double function new name / select existing
* feat(responsive): hide top-bar__secondary-column

## Version 1.8.0
* fix(selection): refactor(layout): merge badge styles and class names
* feat(selection): clicking on buttons-sidebar do not deselect anything previously selected
* fix(selection): double-click report badge, does not close top-bar-reports
* refactor(layout): move titles in reports and filters
* refactor(components): remove duplicated code
* feat(responsive): responsive layout
* feat(responsive): move buttons-sidebar to the bottom of the screen
* feat(responsive): move contents-sidebar to the bottom of the screen, above buttons-sidebar
* feat(responsive): hamburger menu for navbar-menu-group
* feat(responsive): gallery
* feat(responsive): sidebar__section
* feat(responsive): navbar-menu-group
* feat(responsive): auth card

## Version 1.7.0
* refactor(layout): remove sidebar background and border
* refactor(layout): paddings and gaps
* refactor(layout): buttons-sidebar
* refactor(layout): feature-bar__secondary-column
* refactor(layout): feature-bar__secondary-column scrollable
* refactor(layout): feature-bar look like a sidebar__section
* feat(layout): buttons-sidebar expand section name on hover
* feat(layout): app-workspace
* refactor(layout): reduce nesting
* refactor(layout): remove obsolete reports and filters sidebar__section
* refactor(layout): contents-sidebar width
* refactor(layout): top-bar__secondary-column
* refactor(layout): remove legacy-selection-box
* refactor(layout): merge badge styles and class names
* refactor(layout): align right badges in top-bar
* refactor(layout): swap top-bar-report and top-bar-filter columns
* refactor(layout): badges
* refactor(layout): top-bars
* refactor(layout): gaps
* refactor(layout): top-bar--two-column height
* refactor(layout): workspace-body gap
* refactor(layout): contents-sidebar

## Version 1.6.1
* refactor(app): delete legacy files
* refactor(css): users-list and action-buttons styles
* refactor(css): users-list.css

## Version 1.6.0
* refactor(css): feature-bar-title
* refactor(css): borders
* refactor(css): spinner
* refactor(css): sidebar and main-content scrollable
* refactor(css): top-bar-filter and top-bar-report
* refactor(css): remove duplicated stylesheets link
* refactor(css): merged variables in _variables.css
* refactor(css): rename image_gallery.css
* refactor(css): reports
* refactor(css): REFACTORABLE
* refactor(css): DELETABLE
* refactor(css): _components.css
* refactor(css): css overhaul

## Version 1.5.0
* feat(test): TagCrudIntegrationTest, FilterCrudIntegrationTest, ReportCrudIntegrationTest
* fix(warnings): Unused schema declaration
* fix(warnings): Unresolved view reference
* fix(warnings): Unused global symbol
* fix(warnings): Redundant 'if' statement
* fix(warnings): Constant values
* fix(warnings): Performance
* fix(warnings): Java language level migration aids
* fix(warnings): Redundant 'throws' clause
* fix(warnings): Method parameter always has the same value
* fix(warnings): Default annotation parameter value
* fix(warnings): Redundant local variable
* fix(warnings): Subsequent steps can be fused into Stream API chain
* fix(warnings): Method can be extracted
* fix(warnings): Associate new label for inputs
* fix(warnings): Verbose or redundant code constructs
* fix(warnings): Redundant suppression
* fix(warnings): removed unnecessary imports* fix(metadata): corrected the unchecked operation warning in MetadataService.java by specifying the generic types for the deserialized map
* fix(reports): corrected the createShareableLink method in SharedReportService.java to prevent the unique constraint violation
* feat(test): ReportCrudIntegrationTest
* feat(test): FilterCrudIntegrationTest
* feat(test): TagCrudIntegrationTest

## Version 1.4.1
* fix(users): Element form is not closed. Closing tag matches nothing
* fix(filters): one-to-on filters-shared_filters relationship
* fix(users): single line user management

## Version 1.4.0
* feat(aws): 2-tier deployment

## Version 1.3.1
* fix(gallery): image-card to get data from exiftoolData map
* fix(import): update image.exiftool.display-metadata-keys=
* fix(gallery): sort Filename
* fix(gallery): show Filename
* fix(sql): update IMAGES column names
* feat(sql): add migration, schema and data sql scripts

## Version 1.3.0
* fix(tests): application-test.properties file to use the new map-based format
* refactor(reports): compact report layout
* fix(metadata): Descripcion
* feat(metadata): update compact.html, extended.html and show.html templates to obtain the data using the same method as metadata section side__bar
* fix(prod): prevent data loss spring.jpa.hibernate.ddl-auto=validate
* fix(metadata): remove debugging logs
* feat(metadata): do not show empty values
* feat(metadata): image.exiftool.display-metadata-keys extracted from many sources
* feat(metadata): image.exiftool.display-metadata-keys extracted from many sources
* feat(tags): extracted from many sources: XMP-lr:HierarchicalSubject, XMP-tf:Tags,XMP-dc:Subject,IPTC:Keywords
* update(filters): shared filter according last changes
* feat(sort): default sorting imported-descending
* feat(images): image-card-details dynamically constructed from app.sortable-fields
* fix(filters): RATING comparator
* feat(filters): added IMPORTED to filtering
* feat(sort): app.sortable-fields: Name, Rating, Created, Modified, Imported
* feat(sort): app.sortable-fields json map to allow multiple data sources
* feat(seeder): delete all originals, thumbnails and deleted images
* feat(images): images/show.html template

## Version 1.2.1
* fix(reports): share-report-btn can't access property "writeText", navigator.clipboard is undefined
* fix(filters): share-filter-bar-btn can't access property "writeText", navigator.clipboard is undefined

## Version 1.2.0
* feat(filters): updated isValidFilterExpression in filtering.js to incorporate new validation logic
* fix(test): ImagesCrudIntegrationTest.java adapted to filename_yyyymmddhhmmss.jpg /deleted
* fix(filters): Error: Filter evaluation failed: 500
* Merge branch 'warnings-20251212' into dev
* feat(delete): deleted images are saved in /deleted with a datetime suffix
* fix(warnings): 4th round revert problematic changes
* fix(warnings): 3rd round
* fix(warnings): second round
* fix(warnings): first round
* feat(tests): CRUD test for images management ImagesCrudIntegrationTest
* feat(seeder): move all images from /originals to /deleted and delete /thumbnails
* feat(seeder): move all images from /originals to /deleted and delete /thumbnails
* feat(delete): save a copy of deleted images in /storage/deleted
* feat(deploy): show git log after pull

## Version 1.1.0
* refactor(js): finished
* refactor(js): Uncaught TypeError: window.loadSavedFilter is not a function
* refactor(js): refactor to remove intellij warnings
* refactor(js): save-filter-btn and share-filter-bar-btn functionality restored
* refactor(js): save-filter-btn and share-filter-bar-btn functionality restored
* refactor(js): remove debugging console logs
* refactor(js): filtering.js to handle dragging filter-controls to filter-expression-container
* refactor(js): reports.js to include the missing initialization blocks
* refactor(js): friendly warning when uploads exceeding limits
* refactor(js): uploads frindly warning when exceeding limits
* refactor(js):removed obsolete image-show.js
* refactor(js):removed obsolete home-index.js
* refactor(js): compare uploading images with the ones already in the app
* refactor(js): show upload spinner
* refactor(js): update js imports in html templates
* refactor(js): new javascript file structure

## Version 1.0.1
* refactor(reports): report-default.js more secure, better ux
* refactor(images): image-show.js_deletable
* refactor(app): CSRF headers handled only in app-globals.js
* feat(tests): tests for users CRUD
* feat(tests): testLogging { events "passed", "skipped", "failed" }
* fix(navbar): Namespace 'sec' is not bound. Removed not-implemented options in edit menu
* fix(users): delete-user-button in admin's user-list-container

## Version 1.0.0
* fix(version): version incorrectly rendered
* fix(version): version incorrectly rendered
* fix(version): version incorrectly rendered
* fix(production): avoid copying sample_images in production docker image
* fix(version): version incorrectly rendered
* deploy: Tagfolio v1.0.0
* fix(seed): seed profile is only activated when the dev profile is active
* feat(test): tests update to use H2 database
* feat(prod): updated version to 1.0.0-SNAPSHOT
* fix(prod): version stored in just one place, in build.gradle
* fix(prod): EssentialDataSeeder.java to seed ROLES, PERMISSIONS and REPORT_TYPES
* feat(prod): EssentialDataSeeder.java to seed ROLES, PERMISSIONS and REPORT_TYPES
* fix(prod): CreateAdminUser.java creates also ADMIN role
* fix(prod): WelcomeController.java checks current environment to wait or not for the seeder to finish
* feat(prod): admin creation script CreateAdminUser
* feat(prod): separated deva dn prod environments. application-dev.properties application-prod.properties
* feat(prod): changed app Dockerfile to use a built .jar file. Created deployment script deploy.sh to

## Version 0.21.0
*   feat(selection): select images and tags at the same time to delete tags from images
*   refactor(filter): rearrange buttons and fields
*   fix(filter): clear-filter-btn produces duplicated images in image-grid-main
*   refactor(app): remove console logs
*   refactor(report): images into-bar-report-generator zoomable
*   feat(report): remove-selected-images-btn to remove selected images from reports
*   fix(report): removed placeholder-text and rearrange buttons
*   feat(report): save changes before sharing
*   feat(import): conflicts uploading images shown in modal with overwrite all / cancel all buttons
*   feat(import): conflicts uploading images shown in global-alert-container
*   feat(report): removed debugging console logs
*   fix(filter): share-filter-bar-btn copies only the token.
*   refactor(alerts): global alert moved to bottom-right
*   refactor(app): remove backend logger.info
*   refactor: convert System.out.println to logger.info
*   fix(delete): delete-selected-btn asking for confirmation but doing nothing
*   fix(delete): remove debugging console logs
*   feat(sidebar): added 'no filters' and 'no tags' badge

## Version 0.20.4
*   feat(report): default report names
*   fix(seeder): default filter naming uses only basic ascii chars
*   fix(filters): default naming uses only basic ascii chars
*   feat(filters): default naming
*   feat(filters): default naming
*   fix(filters): style filter-name-input
*   feat(filters): added filter-name-input to save or update filter name
*   fix(filters): prevent saving duplicated filters
*   feat(anonymous): let export images
*   feat(anonymous): prevent filter saving or sharing
*   fix(anonymous): align anonymous' interface with user's

## Version 0.20.3
*   refactor(report): center images in report-images-container
*   refactor(filter): move filter-controls-and-actions to the same line of filters-action-buttons, to the left
*   refactor(style): apply border-radius: var(--border-radius-md) to top-bar-report-generator, report-images-container, filter-expression-container

## Version 0.20.2
*   feat(filters): double-click filter loading in filter-expression-container
*   fix(view): list view working again
*   feat(reports): drag report badge in report-images-container for edit
*   feat(reports): double-click report loads it on report-images-container for edit
*   feat(reports): one-step sharing
*   fix(reports): keep sorting order when saving
*   fix(reports): delete dangling placeholder javascript
*   refactor: rename legacy files _borrable

## Version 0.20.1
*   refactor(reports): remove report-images-container's placeholder-text
*   refactor(reports): remove generate-report-btn from sidebar__section
*   feat(reports): adding images to report

## Version 0.20.0
*   feat(reports): sorting images in reports
*   feat(reports): save btn to create and update reports
*   feat(reports): one-step report generation
*   feat(reports): name input and type dropdown
*   feat(reports): separate save and share functionality
*   feat(reports): separate save and share functionality
*   feat(reports): separate save and share functionality
*   * Merge remote-tracking branch 'origin/reports' into reports
*   feat(reports): Add button to add selected images
*   feat(reports): top-bar-report-generator and top-bar-report-generator not mutually exclusive
*   feat(reports): renewed report generation interface similar to filters
*   refactor(reports): renewed report generation interface similar to filters

## Version 0.19.4
*   fix(filters): anonymous user see only filters and tags sidebars
*   fix(filters): Error saving and sharing filter: TypeError: window.handleShareFilterClick is not a function
*   fix: duplicated images in gallery
*   fix(filters): share-filter-bar-btn copies only the token.
*   fix(filters): share-filter-bar-btn reference error
*   fix(filters): save-filter-btn saving filter twice
*   refactor(filters): new-filter-input inside filter-expression-container as the last filter-badge
*   refactor(filters): top-bar-filter extracted to own dedicated files

## Version 0.19.3
*   **refactor(filters)**: one-step filter saving
*   **refactor(filters)**: filter controls moved to top-bar-filter
*   **refactor(filters)**: filter controls moved to top-bar-filter
*   **fix(gallery)**: duplicated images
*   **fix(images)**: remove console logs
*   **fix(images)**: /images/{id} show tags
*   **refactor(images)**: /images{id} page layout and styling

## Version 0.19.2
*   **fix(tags)**: new-tag-input working again and styled
*   **fix(layout)**: .sidebar__section.collapsed min height
*   **fix(scroll)**: metadata sidebar__section horizontal scroll
*   **fix(scroll)**: sidebar__section horizontal scroll visible without the need of scrolling down
*   **fix(selection)**: selection of images maintained when clicking on sidebar__section-title
*   **refactor(sidebar)**: sidebar__section min height
*   **refactor(sidebar)**: sidebar__section max height and vertical scroll
*   **refactor(exiftool)**: smaller monospaced font
*   **feat(alerts)**: global-alert-container in main-content-area top-right part
*   **feat(alerts)**: alerts in global-alert-container disappear after 5 seconds

## Version 0.19.1
*   **feat(layout)**: top-bar-filter hide/unhide when filters sidebar__section is collapsed/uncollapsed
*   **feat(upload)**: spinner and border in drag-n-drop import
*   **fix(layout)**: pagination with filters and sorting fills the grid
*   **fix(layout)**: pagination with filters and sorting
*   **refactor(layout)**: sidebar and main-content scrollable. Gallery pagination linked to main-content scroll
*   **refactor(layout)**: removed bottom-bar
*   **refactor(layout)**: app version in the top-bar
*   **refactor(layout)**: top-bar and bottom-bar fixed
*   **refactor(welcome)**: .btn-primary and .btn-secondary bigger fonts
*   **refactor(register)**: /register only for USER's registration, not ADMIN's
*   **refactor**: Welcome buttons <button> instead of <a>

## Version 0.19.0 Users CRUD
*   **feat(register)**: h2 just 'Register'
*   **feat(navbar)**: 'Change Password' hides unnecessary sections
*   **feat(navbar)**: unified navbar dropdown styles
*   **feat(users)**: inline CRUD for users
*   **feat(users)**: redirect flash messages to global-alert-container
*   **feat(users)**: user inline creation
*   **feat(users)**: styling of user management form
*   **feat(users)**: 'Manage Users' option in ADMIN user-profile-dropdown
*   **fix(users)**: list table shows Updated At
*   **feat(users)**: user management disables sidebar
*   **feat(users)**: CRUD menu for Users management

## Version 0.18.0 Global Refactor
*   **refactor**: remove debugging browser console logs
*   **refactor**: Separate Inline JavaScript from HTML Templates
*   **refactor**: Separate Inline Styles from Error Pages
*   **fix**: Correct upload overlay visibility
*   **refactor**: Standardize Naming Conventions and Separate Inline Code
*   **fix**: Resolve N+1 Query and Duplicate
*   **refactor**: Implement Global Exception Handling and Finalize Controller Refactoring
*   **refactor**: Refine Configuration and Utility Layers
*   **fix**: moving TAG_SOURCE_KEYS to AppConfig.java and isAnonymous is not defined errors
*   **refactor**: Streamline Configuration Layer
*   **refactor**: Streamline Repository Layer
*   **fix**: updated app.js to use the correct endpoint /api/reports/generate
*   **refactor**: Restructure and Refine Controller Layer
*   **refactor**: Refactor Service Layer for Modularity and Consistency
*   **refactor**: Organize JavaScript files and clean up references
*   **feat**: Major refactoring and test suite stabilization

## Version 0.17.4
*   **fix(reports)**: clicking shareReportBtn a second time just copies URL in the clipboard
*   **refactor(reports)**: save and share buttons moved
*   **feat(reports)**: body gap to show report pages
*   **fix(filters)**: see sorting options in shared filters url
*   **feat(seeder)**: add seeder console logs

## Version 0.17.3
*   **feat(reports)**: extended reports A4 layout
*   **feat(reports)**: extended reports layout
*   **feat(reports)**: compact reports styling gaps between items
*   **feat(reports)**: compact reports pagination printing in pdf
*   **feat(reports)**: compact reports pagination
*   **feat(reports)**: compact images square
*   **feat(reports)**: compact report shows original images
*   **feat(reports)**: styling for DIN-A4 layout

## Version 0.17.2
*   **fix(filters)**: anonymous users can filter shared filter's images
*   **fix(filters)**: sharing filtered images with anonymous users

## Version 0.17.1
*   **fix(filter)**: removing a filter does not add images to the report
*   **fix(reports)**: Could not find .right-column or .topBarFilter
*   **fix(pagination)**: when changing from list to grid view, load enough batches to at least fill the grid
*   **fix(selection)**: mays-click selection regardless matching/not-matching state
*   **fix(tags)**: newTagInput only visible when images are selected
*   **fix(sorting)**: importing images with a filter and then changing sorting criteria
*   **fix(sorting)**: sort matching images first, sorted according sorting criteria and not-matching images afterwards also sorted
*   **fix(sorting)**: sort matching images first, not-matching images afterwards

## Version 0.17.0 Pagination
*   **fix(sorting)**: sort matching/not-matching independently maintaining sorting criteria
*   **fix(sorting)**: with filters, sort matching images first and then not-matching
*   **fix(zoom)**: zoom working in list-view
*   **fix(sort)**: refresh checkmark when changing sorting criteria
*   **fix(sort)**: sorting images without valid value at the end
*   **fix(sort)**: sorting with pagination
*   **fix(list-view)**: unified to grid-view. Different layouts via javascript
*   **fix(images)**: double-click to redirect to /images/id
*   **fix(tags)**: when filter is applied and no image is selected, tag-counters relate to matching images
*   **fix(pagination)**: filters to take into account not loaded images
*   **fix(pagination)**: load enough batches to fill the grid
*   **fix(pagination)**: tags refer to all images, even the not yet loaded images
*   **feat(performance)**: infinite scroll. loading enough batches until the whole screen is covered
*   **feat(performance)**: infinite scroll. Images in gallery loaded in batches as user scrolls down
*   **feat(performance)**: show exiftool and metadata of selected images only. No selection, no data shown

## Version 0.16.0 Report config in main view
*   **fix(filters)**: Grouping matching first and not-matching later when report-display-container is shown.
*   **refactor(layout)**: Paddings of elements in right-column.
*   **feat(reports)**: Report-display-container hides when reports sidebar-section is collapsed.
*   **refactor(styling)**: Report-display-container look like gallery sidebar-section.
*   **refactor(styling)**: Report-display-container and topBarFilter to look like sidebar-section.
*   **feat(reports)**: Refresh leftSidebar when report is created.
*   **feat(reports)**: Reports image-cards draggable.
*   **feat(reports)**: Reports image-cards draggable in right-column.
*   **refactor(layout)**: Filter-controls layout.
*   **refactor(layout)**: Reports above filters. Comparator fields in one line.
*   **feat(filters)**: Filter type MODIFIED <= 2001-08.
*   **feat(filters)**: Filter type CREATED = 2001-08.
*   **feat(filters)**: CREATED and MODIFIED comparators and filtering.
*   **fix(styles)**: a:hover text-decoration:none.
*   **feat(filters)**: Maintain filters when changing list/grid view.
*   **feat(filters)**: Filtering in list view.
*   **fix(filters)**: Operators and comparators combined filters.
*   **fix(zoom)**: Maintain zoom when changing from grid to list view.
*   **fix(list-view)**: Show content in the leftSidebar with the same logic as grid-view.
*   **fix(zoom)**: Same size images in list and grid view.

## Version 0.15.0 Comparator filtering
*   **fix(filters)**: Shared comparator filter now correctly excludes images lacking the comparing field.
*   **fix(filters)**: Improved default filter name generation for rating filters (e.g., RATING_>=_3).
*   **fix(filters)**: Filter-badge field literal now remains constant, uppercase, and independent.
*   **fix(filters)**: Revised automatic inclusion of AND operator in filter expressions.
*   **feat(filters)**: Added `>=` and `<=` comparators.
*   **feat(filters)**: Implemented styling for filter-badge field and filter-badge value.
    **feat(filters)**: Introduced comparator filter for Rating.
*   **feat(filters)**: Comparator-fields are now clickable and draggable to the filter/expression-container.
*   **feat(filters)**: Added Rating as a comparator-field.

## Version 0.14.4
*   **fix(footer)**: show the correct version
*   **fix(filters)**: autocomplete-dropdown visible over sidebar-section edges
*   **feat(filters)**: move newFilterInput from filter-expression-container to sidebar-section filters
*   **feat(filters)**: simpler styling for top-bar-filter
*   **feat(filters)**: same styling for operators, parenthesis and comparators
*   **fix(filters)**: click comparators preceded by parenthesis
*   **feat(filters)**: comparators added to filter-expression by click or drag
*   **feat(filters)**: comparators for numbers and dates
*   **fix(tags)**: tag-counter only for filter matching images

## Version 0.14.3
*   **refactor(reports)**: Generate button restyled as other action-buttons
*   **feat(sidebar)**: Sidebar sections can be collapsed by clicking on the section title text
*   **refactor(filter-bar)**: Grouped action buttons
*   **refactor(filter-bar)**: Moved action buttons into the Filters sidebar-section
*   **refactor(filter-bar)**: Moved the filter bar to the top of the mainContentArea; collapsable together with Filters section
*   **refactor(leftSidebar)**: Collapse toggle button placed to the left of the header
*   **fix(zoom)**: Configurable zoom max, min and step in `_variables.css`
*   **fix(list-view)**: Zoom in/out working in list view
*   **fix(debug)**: `application.properties` allows debug logs
*   **fix(style)**: 300px sidebar and removed padding between tags
*   **fix(style)**: Auth card with larger font and centered buttons

## Version 0.14.2
*   **Merge branch 'dev'**
*   **fix(import)**: spinner style centered and yellow
*   **fix(import)**: drag-n-drop import images
*   **fix(styles)**: remove navbar <a> underline
*   **fix(styles)**: auth-card vertically centered
*   **fix(styles)**: navbar-menu-group buttons style
*   **overhaul(styles)**: refactor styles
*   **refactor(js)**: ui.js merged into navbar.js
*   **fix(navbar)**: recovered user logout, change password
*   **refactor(navbar)**: delete Share menu
*   **fix(grid-view)**: allow sorting
*   **feat(grid-view)**: show only sortable fields
*   **fix(reports)**: remove Edit > Generate Reports
*   **fix(zoom)**: View > Zoom In/Out
*   **fix(zoom)**: View > Zoom In/Out restored
*   **refactor(styles)**: purge, merge and consolidate style sheets
*   **refactor(styles)**: purge unused styles

## Version 0.14.1
*   **refactor(styles)**: extract styles from html to css files
*   **fix(csrf)**: uploadOverlay initially hidden
*   **fix(csrf)**: CSRF token not available in global variables
*   **overhaul(layout)**: fresh look

## Version 0.14.0 Tag export
*   **Merge branch 'dev'**
*   **feat(report)**: Prevent pop-up windows
*   **feat(report)**: Generate button on app.html
*   **feat(report)**: /report/id styling
*   **feat(report)**: /report/id use /reports/config.html to sort images in reports
*   **feat(export)**: add current tags to image metadata in key XMP-tf:Tags as an array
*   **fix(import)**: if no image.exiftool.tag-source-keys, default to XMP-dc:Subject
*   **fix(export)**: export images working again
*   **feat(images)**: delete dangling tags when images are deleted
*   **refactor(layout)**: Filters above Reports
*   **refactor(layout)**: change no reports/filter/tags literals
*   **fix(images)**: store as separate columns only sortable fields. The rest will be extracted from IMAGES.EXIFTOOLOl
*   **fix(reports)**: extract and store in the database the keys needed for sorting
*   **fix(reports)**: extract data using new Exiftool JSON keys
*   **feat(exiftool)**: get structured JSON instead of flat
*   **delete(legacy)**: remove legacy files HomeController.java, PasswordService.java, reports/index.html, settings/password.html, settings/profile.html
*   **refactor(logs)**: remove debugging console logs

## Version 0.13.4
*   **Merge branch 'dev'**
*   **fix(filters)**: error generating a shareable filter
*   **refactor(rightContentSidebar)**: exiftool data shown as JSON
*   **refactor(navBar)**: removed Show > Exiftool/Metadata/Tags
*   **refactor(navBar)**: removed Show > Exiftool/Metadata/Tags
*   **fix(rightSideBar)**: show multiple images Metadata and Exiftool data
*   **feat(rightSideBar)**: collapsible sections
*   **fix**: some warnings on initial view
*   **refactor(layout)**: remove debugging logs
*   **refactor(layout)**: added Metadata and Exiftool sections to rightContentSidebar
*   **refactor(layout)**: rightContentSidebar structured in sections
*   **refactor(reports)**: update templates to look like the examples
*   **refactor(reports)**: make report templates separate files
*   **refactor(filters)**: not-matching images darker
*   **feat(tests)**: Tests for user register, login, logout, change password
*   **refactor(seeder)**: delete legacy DataSeeder.java
*   **refactor(seeder)**: filters and reports default name and shareable
*   **refactor(filters)**: share filters url /shared/filter/hash
*   **feat(reports)**: share hashed reports
*   **fix(reports)**: generate reports
*   **fix(filters)**: delete shared filters
*   **fix(filters)**: show share button
*   **deploy**: Tagfolio v0.13.3

## Version 0.13.3
*   **Merge branch 'dev'**
*   **fix(delete): delete tags refresh tags suggestion**
*   **fix(delete): delete tags working**
*   **fix(delete): refresh view after filter deletion**
*   **fix(delete): send confirmation messages to globalAlertContainer**
*   **refactor(delete): consolidate all deletion methods in Edit > Delete**
*   **fix(selection): all selection methods working**
*   **fix(selection): some selection methods**
*   **refactor(selection): consolidating all selection logic on selection.js**
*   **feat(reports)**: styling as filters and tags
*   **feat(reports)**: shown on rightContentSidebar
*   **fix(delete): deleted images are deleted from containing reports**
*   **feat(delete): delete filters**
*   **feat(delete): delete filters**
*   **refactor(delete): merge Edit > Delete Images/Tags/Filters/Reports into Edit > Delete**
*   **refactor(export): merge File > Export > Images/Filters/Reports into Export Images**
*   **fix(selection): mays+click selection in tags**
*   **fix(selection): images, tags and filters selection working**
*   **fix(selection): selection of images, tags, filters, reports, mutually exclusive**
*   **refactor(selection): selection of images, tags, filters, reports, mutually exclusive**
*   **refactor(export): name exported file tagfolio-images.zip**
*   **feat(images)**: File > Export > Filters and File > Share > Filters
*   **feat(images)**: File > Export > Images
*   **refactor(navbar)**: remove unnecessary options
*   **refactor(ui)**: remove mobile's implementation. leave only desktop's**

## Version 0.13.2
*   **fix(seeder)**: use NUM_FILTERS_PER_USER
*   **fix(seeder)**: seed reports
*   **refactor(logs)**: removed unnecessary logs
*   **feat(seeder)**: seed filters and reports
*   **refactor(logs)**: removed unnecessary debugging logs
*   **fix(filters)**: dragging several filters bug
*   **feat(filters)**: OR where needed for dragged filters
*   **feat(filters)**: AND where needed for dragged tags
*   **feat(filters)**: AND before dragged tag if necessary
*   **feat(filters)**: draggable operators and parenthesis
*   **feat(filters)**: select many and Edit > Delete filters
*   **refactor(filters)**: look like tags. share button inside
*   **fix(selection)**: single-click outside to unselect all items
*   **fix(selection)**: single-click toggle selected/unselected. double-click redirects to /images/id
*   **refactor(selection)**: remove drag-to-select selection
*   **refactor(tags)**: tags will be extracted only from Exiftool Subject field

## Version 0.13.1
*   **feat(sorting)**: Images without sorting field are sorted last.
*   **refactor**: Removed obsolete fragments.
*   **feat(sorting)**: Added sorting criteria to `application.properties` (`app.sortable-fields`).
*   **feat(tags)**: Counter reflects the tag count in the selection of images (including for anonymous users).

## Version 0.13.0 Import conflict detection
*   **feat(upload)**: Implemented a robust conflict detection and resolution system for image uploads.
*   **feat(upload)**: Users are now prompted to overwrite or cancel when an uploaded image has the same name but different metadata as an existing one.
*   **feat(upload)**: The comparison logic now focuses only on relevant metadata fields defined in `image.exiftool.tag-source-keys`, preventing false conflicts.
*   **fix(upload)**: Corrected an issue where uploading the same image twice would trigger a conflict instead of being skipped.
*   **fix(seeding)**: Standardized the image creation process between the database seeder and regular uploads to prevent metadata inconsistencies.
*   **refactor(upload)**: Refactored `ImageService` to introduce a centralized `createImageFromFile` method, ensuring consistent image and metadata handling.
*   **refactor(upload)**: Updated `DatabaseSeeder` to use the new standardized image creation service.
*   **fix(build)**: Resolved multiple compilation errors related to incorrect type conversions and missing symbols in `ImageService` and `ImageController`.
*   **fix(build)**: Addressed deprecation warnings in `SecurityConfig` by replacing `AntPathRequestMatcher` with the recommended `requestMatchers` method.

## Version 0.12.1
*   **fix(layout)**: Corrected horizontal alignment of images in the gallery view to ensure they are properly centered.
*   **fix(layout)**: Adjusted the right sidebar layout to prevent the "Share" button from being pushed out of view by long filter names.
*   **feat(layout)**: Changed the background color of the right sidebar to match the main content area for a more unified look.
*   **feat(layout)**: Aligned the content of the right sidebar (filters and tags) to the right margin.
*   **fix(layout)**: Removed the border between the main content area and the right sidebar.
*   **fix(ui)**: Changed the "Copy URL" button text to "Share" for clarity.

## Version 0.12.0 Filters update
*   **feat(layout)**: Centered upload spinner.
*   **feat(filters)**: Added one-click Export button.
*   **fix(layout)**: Improved paddings and pill shape for save-filters-list.
*   **feat(layout)**: Removed left sidebar, added filters to topbar, and moved Saved Filters to the right sidebar.
*   **feat(layout)**: Simplified layout (not responsive).
*   **feat(reports)**: Shorter URL for reports (`/reports/id`).

## Version 0.11.1

*   **feat(reports)**: Changed "File > Generate Report" to a submenu with "Compact" and "Extended" options, directly generating a static HTML view.
*   **feat(reports)**: Implemented static report generation using HTML templates stored in the `report_types` table.
*   **feat(reports)**: Created a new endpoint `GET /reports/{id}/{type}` to serve the generated static HTML reports.
*   **feat(reports)**: Implemented a new `report_types` table and `ReportType` entity to store configurable report templates.
*   **feat(reports)**: Added drag-and-drop and button-based reordering of images within the (now removed) interactive report view.
*   **feat(reports)**: Refactored the `report_images` relationship to an intermediate entity to store a `sorting_order` for each image in a report.
*   **feat(reports)**: Implemented access control to restrict viewing of reports to their creators and ADMIN users.
*   **fix(reports)**: Resolved multiple `LazyInitializationException` and `TemplateInputException` errors related to report generation and viewing.
*   **refactor(reports)**: Removed the interactive report detail page (`/reports/{id}`) and its associated backend logic in favor of the direct static view.

## Version 0.11.0 Reports

*   **feat(reports)**: Implemented "Generate report" functionality to create a webpage (`/reports/{id}`) from selected images.
*   **feat(reports)**: Added new `Report` entity, `ReportRepository`, and `ReportService` for report management.
*   **feat(reports)**: Created `ReportController` with endpoints for generating (`POST /reports/generate`) and viewing (`GET /reports/{id}`, `GET /reports`) reports.
*   **feat(reports)**: Developed `report-detail.html` and `reports/index.html` templates for displaying report details and a list of user reports.
*   **feat(reports)**: Changed Report ID type from `UUID` to sequential `Long` integers.
*   **feat(reports)**: Report creation prompt now suggests a default name in `yyyy-mm-dd-hh-mm-ss` format.
*   **fix(backend)**: Corrected `Image` ID type handling in report generation from `UUID` to `Long`.
*   **fix(backend)**: Ensured `UserRepository.findByEmail` is used consistently instead of `findByUsername`.
*   **fix(backend)**: Resolved `LazyInitializationException` during JSON serialization by adding `@JsonIgnore` to `Report.user`, `Image.user`, and `Tag.images`.
*   **fix(backend)**: Resolved `LazyInitializationException` during Thymeleaf rendering by explicitly initializing lazy-loaded collections (`Report.user`, `Report.images`, `Image.tags`, `Tag.images`) in `ReportService.getReportById`.
*   **fix(backend)**: Added `serveThumbnailImage` endpoint (`GET /images/thumbnail/{id}`) to `ImageController` to correctly serve image thumbnails.
    **fix(backend)**" Handled `IOException` in `ImageController.serveThumbnailImage` and `serveOriginalImage` methods.
*   **fix(config)**: Added `storage.thumbnails-path` configuration to `application.properties`.
*   **feat(ui)**: Report detail page (`report-detail.html`) now uses `report.user.email` for displaying the creator.
*   **feat(ui)**: Report detail page (`report-detail.html`) is styled consistently with the application's dark theme by adding `.card` styles to `app.css`.
*   **feat(ui)**: Report detail page (`report-detail.html`) no longer displays `image-tags-overlay`.
*   **fix(ui)**: Report generation now opens in the same browser tab.
*   **refactor(ui)**: Reverted changes to `selection.js` related to `Ctrl+Click` image selection.

## Version 0.10.1

*   **feat(sharing)**: Implemented content-based hashing for shared filters to enable deduplication.
*   **fix(backend)**: Renamed `SharedFilter.hash` to `SharedFilter.token` and added `SharedFilter.contentHash` for proper shared link management.
*   **fix(backend)**: Updated `SharedFilterRepository` with `findByContentHash` and `findByFilterId` methods.
*   **fix(backend)**: Modified `SharedFilterService` to use `contentHash` for deduplication and `token` for shareable links.
*   **fix(backend)**: Modified `FilterController.getSavedFilters` to return `FilterResponse` DTOs including shared status and shareable link.
*   **fix(backend)**: Modified `ImageController.getSelectedImagesTags` to correctly calculate tag counts and common tags for anonymous users in shared views.
*   **fix(backend)**: Modified `ImageController.filterImages` to accept `FilterRequest` DTO for sub-filtering shared image sets.
*   **fix(security)**: Added `/images/filter` to `publicApiSecurityFilterChain` to allow anonymous access for sub-filtering.
*   **feat(ui)**: Anonymous users can now perform sub-filters on shared image sets using the filter bar.
*   **feat(ui)**: Displayed tags for anonymous users in shared views now correctly show counts relative to the shared image set.
*   **feat(ui)**: Added a "Copy URL" button next to shared filters in the saved filters list for registered users.
*   **fix(ui)**: Corrected `app.js` to properly render the "Copy URL" button by checking `filter.shared` from the backend response.
*   **fix(ui)**: Ensured `saved-filters-list` reloads after a filter is shared to immediately display the "Copy URL" button.
*   **refactor(ui)**: Added basic CSS styling for the `.share-filter-btn` to make it visible and user-friendly.

## Version 0.10.0 Shared filters

*   **feat(sharing)**: Anonymous users can view shared filter images and their tags.
*   **fix(security)**: Corrected `SecurityConfig` to allow anonymous access to `/images/tags` and `/api/filters` endpoints.
*   **fix(security)**: Refactored `SecurityConfig` to correctly order filter chains using `NegatedRequestMatcher`.
*   **fix(backend)**: Handled `NullPointerException` in `ImageController.getSelectedImagesTags` for anonymous users.
*   **fix(backend)**: Handled `NullPointerException` in `FilterController.getSavedFilters` for anonymous users.
*   **feat(ui)**: Hidden "View > Exiftool/Metadata" menu items for anonymous users.
*   **feat(ui)**: Set "View > Tags" as the default sidebar view for anonymous users.
*   **feat(ui)**: Updated anonymous user display in the topbar to match authenticated user format.
*   **fix(ui)**: Disabled tag input for anonymous users.

## Version 0.9.2

*   **fix(filters)**: Dragged filters are part of the filtering expression.
*   **feat(filters)**: Saved filters can be dragged to be reused in a new filter.
*   **refactor(filters)**: Save filters with simple ASCII chars.
*   **feat(filters)**: Save functionality.
*   **feat(tags)**: `rightContentSidebar` newTagInput shows autocompletion and suggestions.
*   **refactor(filters)**" Autocomplete-dropdown styling.
*   **feat(filters)**: Autocomplete-dropdown keyboard accessible.
*   **fix(filters)**: Add AND only between tags.

## Version 0.9.1

*   **fix(filters)**: newFilterInput correctly detects suggested tag
*   **fix(filters)**: positioning of newFilterInput
*   **fix(images)**: image deletion
*   **refactor**: app.html split in partials html, css, and js

## Version 0.9.0 Filter input

*   **feat(filters)**: writing pill
*   **feat(filters)**: writing filter + dragging tags
*   **feat(filters)**: directly writing in filterExpressionInput
*   **feat(seeding)**: waiting page for seeding to complete

## Version 0.8.0 Filter creation

*   **feat(filters)**: Images can be selected/unselected in filter mode.
*   **fix(filters)**: Drag several tags, remove invalid filter warning.
*   **feat(filters)**: Drag several tags -> chain tags with AND.
*   **fix(filters)**: Incomplete filter, use the last valid filter.
*   **fix(filters)**: All images shown when filter is cleared.
*   **feat(filters)**: Drag tags one by one and use operators.
*   **feat(filters)**: Drag and drop tags.
*   **feat(filters)**: Filters area initial layout.
*   **feat(filters)**: New filter input.
*   **feat(tags)**: `tags/index.html` to separate tags part into a fragment, as filters.
*   **feat(filters)**: `leftContentSidebar` to work with filters.
*   **Merge**: Merge remote-tracking branch 'origin/dev' into dev.
*   **feat(delete)**: Implement Global tag deletion.
*   **feat**: Edit > Delete.
*   **fix(register)**: Assigned role user when registering.
*   **refactor(welcome)**: New coherent look.
*   **refactor(layout)**: New tag creation integrated in tags-list.
*   **refactor(layout)**: Reorder View > Exiftool-Metadata-Tags.
*   **fix(images)**: Single click for select. Double click for redirect to `/images/id`.
*   **fix(login)**: `/` redirects to `/login` if not auth. `/login` redirects to `/` if auth.
*   **refactor**: Simplify layout: heading, footer, main for pictures, aside for tags.
*   **refactor**: Separating `app.html`, `app.css` and `app.js`.

## Version 0.7.0

*   **fix(backend)**: Gracefully handled `NoSuchElementException` in `ImageController.getSelectedImagesTags` when the authenticated user is not found.
*   **fix(redirects)**: Corrected redirects from `/images` to `/` in `ImageController.storeForm` and `ImageController.destroy` to prevent `405 Method Not Allowed` errors.
*   **feat(sorting)**: Changed default image sorting to 'Title' in 'Ascending' order on the homepage.
*   **fix(frontend)**: Resolved `Uncaught ReferenceError: setLeftSidebarOpen is not defined` by ensuring `applySelectedTagsToFilter` is defined within the `DOMContentLoaded` event listener.

## Version 0.6.0 Tags

*   **feat(tags)**: Implemented context-sensitive tag display in the right sidebar: shows all available tags when no images are selected, and only common tags when images are selected.
*   **feat(tags)**: Added multi-selection capabilities for tags in the right sidebar (single click, Ctrl/Cmd+click, Shift+click).
*   **feat(tags)**: Consolidated tag deletion into the "Edit > Delete" menu option, which now intelligently deletes selected tags (if any) or selected images (otherwise).
*   **feat(tags)**: Added "Filter by Selected Tags" button in the right sidebar to apply an AND filter to the /filters view based on selected tags.
*   **feat(filters)**: Enhanced /filters page to correctly process multiple initial tags from URL parameters, applying an AND filter and displaying them in the filter expression UI.
*   **feat(filters)**: Modified /filters page to always display all accessible images, with filtering handled by frontend JavaScript to correctly populate filtered/unfiltered grids.
*   **fix(backend)**: Corrected `ImageController.getSelectedImagesTags` to properly retrieve common tags for selected images and all tags when no images are selected.
    **fix(backend)**: Ensured `ImageController.getSelectedImagesTags` correctly handles ADMIN permissions for tag visibility.
*   **fix(backend)**: Added `ImageRepository.existsByTagsContaining` to correctly identify and delete orphaned tags after removal.
*   **fix(frontend)**: Resolved JavaScript scope issues (`updateSidebarContent is not defined`) by moving global variables.
*   **fix(frontend)**: Removed redundant "Delete Selected Tags" button from the right sidebar.

## Version 0.5.0

*   **feat(tags)**: Implemented tag creation and association for selected images directly from the right sidebar.
*   **feat(tags)**: Added support for adding multiple comma-separated tags at once.
*   **feat(tags)**: Tag saving process is now triggered by hitting 'Enter' in the tag input field.
*   **feat(tags)**: Tag pills in the right sidebar are now clickable and redirect to the filters page with the tag preloaded.
*   **feat(tags)**: Tags in the right sidebar are sorted by popularity (most used first) and then alphabetically.
*   **fix(ui)**: Adjusted CSS for tag pills to be more compact and fit more tags in the sidebar.
*   **fix(backend)**: Corrected `ClassCastException` when processing image IDs from frontend to backend.
*   **fix(backend)**: Improved robustness of `WelcomeController` to handle missing user gracefully, preventing `NoSuchElementException`.
*   **fix(ui)`: Ensured confirmation messages are not prematurely dismissed by subsequent 'Enter' key presses.

## Version 0.4.2

*   **fix(permissions)**: ADMINs can see all tags and filters
*   **fix(permissions)**: ADMINs can see all images
*   **fix(permissions)**: admin can see all images
*   **refactor**: user-role-permission overhaul

## Version 0.4.1

*   **feat(layout)**: Refactored `left-side-bar` and `right-side-bar` to `leftButtonBar` and `rightButtonBar` for a more streamlined interface.
*   **feat(layout)**: Introduced `leftContentSidebar` and `rightContentSidebar` to provide more flexible content display options.
*   **feat(layout)**: Added "Filters" and "Reports" buttons to the `left-side-bar` for easier access to these features.
*   **fix(layout)**: Improved the visual appearance and alignment of the `top-bar`, `left-side-bar`, and `right-side-bar`.
*   **fix(layout)**: Added a toggle button (`rightContentSidebarToggle`) to the `rightButtonBar` for better control over the `rightContentSidebar`.

## Version 0.4.0

*   **refactor(database)**: Removed redundant `SUBJECT`, `TITLE`, `DESCRIPTION`, `ORIGINALPATH`, and `THUMBNAILFILENAME` columns from `TAGFOLIO.IMAGES` table.
*   **refactor(model)**: `Image` entity now dynamically derives `title`, `subject`, `description`, `originalFileName`, and `thumbnailFileName` from the `EXIFTOOL` JSON data.
*   **refactor(service)**: `ImageService` updated to reflect `Image` model changes and to use configurable ExifTool fields (defined in `image.exiftool.tag-source-keys`) for automatic tag generation.
*   **refactor(repository)**: `ImageRepository` updated to remove methods relying on the removed direct fields.
*   **refactor(controller)**: `HomeController` updated to perform in-memory sorting for 'filename' and 'title' as these are now derived fields.
*   **feat(metadata)**: `ImageController` now filters displayed metadata in image detail views based on configurable keys defined in `image.exiftool.display-metadata-keys`.
*   **fix(ui)**: Removed the "View > Metadata" menu from the navigation bar to simplify the user interface.

## Version 0.3.2

*   **feat(views)**: Consolidated Grid and List views into a single `home/index.html` for improved maintainability.
*   **feat(upload)**: Enabled drag-and-drop image uploads in both Grid and List views with dynamic updates.
*   **feat(upload)**: Implemented a global, centralized spinner for upload feedback, positioned in the `centerArea`.
*   **feat(selection)**: Corrected drag-and-drop selection box behavior to accurately match pointer movement.
*   **feat(selection)**: Ensured single-click on image items triggers selection only, with double-click for redirection.
*   **feat(sorting)**: Renamed 'Name' sort option to 'Filename' for clarity.
*   **feat(sorting)**: Reordered sort options in 'View > Sort' menu to 'Filename', 'Title', 'Date'.
*   **feat(sorting)**: Implemented ascending/descending sort functionality for all sort options.
*   **feat(zoom)**: Added 'Zoom In' and 'Zoom Out' options to 'View' menu to control thumbnail size in both Grid and List views.
*   **feat(metadata)**: Moved metadata filter options from right sidebar to 'View > Metadata' submenu, allowing selection even when no image is selected.
*   **feat(metadata)**: Implemented 'Select All' as a checkbox in the metadata filter menu for better UX.
*   **fix(spinner)**: Corrected spinner display and functionality during uploads in both views.

## Version 0.3.1

*   **chore(release)**: Updated application version to `0.3.1`.
*   **chore(config)**: Configured database seeder for initial production deployment with one admin user.
*   **chore(config)**: Adjusted `application.properties` for production, disabling `ddl-auto`, hiding SQL, and enabling Thymeleaf cache.

## Version 0.3.0

*   **feat(layout)**: Implemented responsive sidebars with resizing and collapse/expand functionality for both wide and narrow screens.
*   **fix(layout)**: Corrected positioning and visibility of sidebar toggles and resizers across all screen sizes.
*   **feat(alerts)**: Moved global alert container to the right-bottom corner of the main content area.

## Version 0.2.1

*   **feat(filters)**: Improved filter building experience with inline tag suggestions and pill-shaped design.
*   **feat(filters)**: Implemented a more intuitive "save mode" for naming and saving filters.
*   **fix(layout)**: Resolved persistent layout issues with header and footer overlap across all views.
*   **feat(layout)**: Improved the app layout to ensure the header and footer do not cover the main content, and the main content is scrollable.
*   **feat(filters)**: Disabled Save button if filter-expression is empty or has a not a valid filter

## Version 0.1.0

*   **feat(tags)**: Refactored tag counting to be dynamic, removing the `UserTag` entity and its counter.
*   **feat(tags)**: Implemented interactive 'Add Tag' and 'Remove Tag' functionality in image detail view.
*   **feat(tags)**" Improved UI/UX for tag management with pill-like input and toggle modes.
*   **fix(database)**: Resolved `LazyInitializationException` and Oracle `GROUP BY` errors in tag queries.
*   **fix(display)**: Ensured only image-specific tags are listed in the image detail view.
*   **fix(permissions)**: Ensured user-specific tags are correctly displayed and filtered in `/filters`.

## Version 0.0.3

*   **feat(permissions)**: Implemented simplified user-centric image and filter permissions.
*   **feat(permissions)**: Users can now only perform CRUD operations on their own images and filters.
*   **refactor(security)**: Removed complex role-based permission checks to streamline the security model.

## Version 0.0.2

*   **feat(images)**: Tags are now clickable and redirect to the filter with the tag preloaded.
*   **feat(filter)**: The size of the tags is now related to their popularity.
*   **feat(filter)**: Tags are now listed in order of popularity.
    **feat(tags)**: Added a `COUNTER` column to the tags table and tags are now stored in lowercase.

## Version 0.0.1

*   **feat(filters)**: A default filter name is now proposed when saving a filter.
*   **feat(filters)**: A new `FILTER` mode has been created, and filters can now be saved.
*   **feat(filters)**: Unfinished filter expressions are now ignored.
*   **Initial version of the Tagfolio application.**
