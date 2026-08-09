# AI Integration Reflection

AI was integrated throughout this project under the Tier C policy. I used ChatGPT mainly during the earlier planning and implementation stages, while Codex was used later to review the existing project, diagnose logical problems and make verified changes in the repository. Instead of asking AI to generate the whole application at once, I worked feature by feature so that I could understand, test and evaluate each suggestion before accepting it.

## What AI Was Most Useful For

AI was most useful when I needed help turning requirements into a structured design and understanding unfamiliar technical areas. It helped me separate the application into models, repositories, services and ScalaFX user-interface classes while following the Scala 3 style used in the lecture material.

A particularly useful example was interaction **#44**, where AI helped improve the transaction handling used for distribution operations. This helped me understand why related database changes should commit together or roll back together instead of being saved independently. Interaction **#46** was also useful when handling older completed distributions and preventing stock from being deducted more than once. These interactions improved both my understanding of Derby transactions and the reliability of the final workflow.

AI was also helpful when debugging problems such as the UTF-8 BOM error, validating inventory input and identifying distribution over-allocation risks.

## Where AI Misled Me

AI output was not always correct even when the code compiled. In interactions **#40** and **#41**, the earlier distribution workflow focused mainly on changing the distribution status and reporting it. This did not fully consider the effect on inventory stock and the associated household request.

During later review, I caught the problem from interaction **#40** by tracing that completion only changed the plan status and did not reduce inventory or synchronise the request. I identified the reporting error from interaction **#41** by checking that the calculation used distinct request IDs, not household IDs. I also noticed that the dashboard added kilograms, bottles and packs into one meaningless total. These checks showed me that compiling successfully does not guarantee correct business logic, so I requested revisions.

## What I Did That AI Could Not

My responsibility was to decide whether the suggestions actually made sense for my project. In interaction **#53**, I reviewed the proposed changes and made the final decisions about how the application should behave rather than accepting every suggestion automatically.

For example, I chose to keep automatic storage instructions for the two `FoodItem` subtypes and use controlled inventory-unit choices because these decisions made the interface clearer. I also manually tested complete workflows, including adding food, approving and rejecting requests, creating distributions, completing or cancelling plans, checking stock changes and confirming that data persisted after restarting.

I recorded significant interactions in `ai/interaction_log.md`, linked assisted sections using `// ai-assisted: #N` comments, reviewed Git changes, compiled the project and ran MUnit tests. Overall, AI acted as a development assistant and reviewer, while I remained responsible for the final design, testing and correctness of the submitted system.
