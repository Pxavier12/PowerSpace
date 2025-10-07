# Powerspace test

In this test, you have to make an Ad Server and a Bidder to simulate an example of Powerspace.
For this you have two repositories, used like a database, to load Positions and AdGroups.

## Reminder
An Ad Server is a system that receives requests from advertisers for ad placements (Positions) and responds with
bids provided by a bidder. An Ad Server is responsible for managing the placement of advertisements on a website 
the quickest and most efficiently as possible. In our case, the Ad Server should always display something.

A bidder is a crucial component in digital advertising that receives bid requests for ad placements (Positions),
evaluates available advertisements (AdGroups) based on various criteria such as targeting, budget, and bid amount, and
then responds with the most suitable advertisement. When a position becomes available, the bidder checks which AdGroups
are eligible, calculates their effective bid values, and selects the highest bidder
while ensuring all campaign constraints are met.

## Todo
 - you have to make an api (like /{position_code}) who requests a position and displays an advertisement.
 - you have to check each AdGroups config to check if they can be displayed and if there is compatible with the position
 - select the AdGroup with the greater cost
 - bill this AdGroup
 - display this AdGroup to the user (name + description + image)

## Requirements
 - Scala 2.13
 - the Adserver should use [Http4s](https://http4s.org/)
 - the bidder should use either http4s or protobuf (best)

## Expectations
The goal is not only to solve what is explained in the todo section, but also to complete it with what you understand
about Powerspace's challenges.

# Feel free to ask any questions at rlavancier@powerspace.com

Enjoy :)
