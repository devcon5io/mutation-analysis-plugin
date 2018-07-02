Sonar Pitest Pro
===============

Sonar Pitest Pro is a commercial plugin for the Sonarqube platform to analyze mutation test results generated from
[Pitest](http://pitest.org). There is also a community plugin for pitest available, which provides basic analysis and
 reporting.
 
Version Comparison
------------------

|                |    Pitest    | Pitest Pro |
|                | (Community)  | Pitest Pro |
|----------------|--------------|------------|
| Rules          |     2        |     21     |
| Mutator Rules  |     0        |     17     |
| Metrics        |     10       |     10     |
| Settings       |     2        |     5      |
| Technical Debt |     no       |     yes    |
| Java Version   |     7        |     8*     |
| Sonar Version  |  <= 5.1**    |  >= 5.2*   |

*) Version prior Java 8 and Sonar 5.2 on demand, extra fees apply
**) PR to support versions from 5.2 is pending

The main difference to the community plugin are the rules. Apart from just having a rule for insufficient coverage
and survived mutations the Pro version has a separate rule for every known mutation including documentation and 
additional rules for uncovered and survived mutants. The rules are overlapping so users have a choice on granularity. 

Terminology
-----------

The Pitest Pro plugin uses a slightly different terminology than the original plugin or pitest itself

|  Pitest     | Pitest Plugin        | Pitest Pro Plugin |
|-------------|----------------------|-------------------|
| Mutator     | Mutator              | Mutagen           |
| Mutant      | Mutations            | Mutant            |
| -           | Survived Mutation    | Survivor          |
| -           | Not Covered Mutation | Lurker            |
| -           | Detected Mutation    | Spotted Mutant    |
| -           | Killed Mutation      | Killed Mutant     |

