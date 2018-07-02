Getting Started
===============

Installation
------------
Install the plugin either using the Sonarqube Update Center or download it manually and copy it into the 
 `extensions/plugins` directory of your sonarqube installation.
 
Plugin Settings
---------------

Rules Configuration
-------------------

After installation you have to enable the rules for your quality profile. You may create a new profile inheriting 
from an existing and enable the rules in the new profile. 

Limit the search to the repository `PitestPro`.

Tags:

- *beta* Rules that are in beta state, are actually not, but the mutator the rule relates to is in experimental state
. So you might not enable those mutator rules, but it does no harm either.
- *mutationOperator* those rule refer to a particular Pitest mutator. If you enable the mutationOperator rules (which is recommended), 
you should disable the rules tagged `aggregate`
- *aggregate* these rules represent an aggregated value. Every rule for a particular mutation is either in state 
*survived*, *lurking* or *unknown*. If you don't want to have an issue for every particular mutant but only an 
aggregated rule for one the 3 states, enable these rule instead of the `mutationOperator` rules. 

Dashboard Widget
----------------
Pitest Pro comes with a dashboard widget `Mutation Coverage`. Just add it to your dashboard. It required no further 
configuration.






