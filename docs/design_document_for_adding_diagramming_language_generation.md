# Design Document for Adding Diagramming Language Generation

<header style="font-family: monospace;">
  <p>Name: Mohamed M. Sallam</p>
  <p>Email: muhd [dot] sallam [at] gmail [dot] com</p>
  <p>LinkedIn: <a href="https://linkedin.com/in/muhd-sallam">linkedin.com/in/muhd-sallam</a></p>
</header>

## TL;DR;
We want to add diagramming language support for StateLang (aka, cc_smc), 
the available diagramming languages for now are Mermaid and PlantUML. In 
this document we will discuss each and their features and how to generate
code in these languages.

## Perquisites
1. Skim the document before fulfill the below perquisites, so
   you have an idea what to focus on during the fulfillment of the 2nd
   perquisite.
2. Watch 28, 29 and 30 videos of Clean Code Video Series by Uncle Bob 
   **and** read the source code during watching.
3. Read cc_smc `README.md` file.

## Important Backgrounds
### Semantic DSs
There are two Semantic Data Structures produced by StateLang: 
1. `SemanticStateMachine`:
    - Produced by `SemanticAnalyzer`.
    - A structure mapping a human-intuitive valid StateLang syntax. 
      This means that all abstract and super states, entry and exit actions,
      and multiple inheritance **won't** be reduced to more simple, but
      non-human-intuitive, states.  
      Example:
      ```
      {  
        (ib1) >ib1x - - -  
        (ib2) : ib1 >ib2x - - -  
        (ib3) : ib1 >ib3x - - -  
        (sb1) <sb1n - - -  
        (sb2) :sb1 <sb2n - - -  
        (sb3) :sb1 <sb3n - - -  
        i:ib2 :ib3 >x e s a  
        s :sb2 :sb3 <n e i -
      }
      ```
      This code will be stored in our `SemanticStateMachine` DS as it is.
2. `OptimizedStateMachine`:
    - Produced by `Optimizer`.
    - A structure mapping an optimized version of the valid user code.
      This means that all abstract and super states, entry and exit actions,
      and multiple inheritance **will be** reduced to more simple, but
      non-human-intuitive, states.  
      Example:  
      This is the human-intuitive code -I know this is not look like that 
      but this is because I didn't use a real-world example-:
      ```
      {  
        (ib1) >ib1x - - -  
        (ib2) : ib1 >ib2x - - -  
        (ib3) : ib1 >ib3x - - -  
        (sb1) <sb1n - - -  
        (sb2) :sb1 <sb2n - - -  
        (sb3) :sb1 <sb3n - - -  
        i:ib2 :ib3 >x e s a  
        s :sb2 :sb3 <n e i -
      }
      ```
      `Optimizer` will reduce it to `OptimizedStateMachine` DS which is 
      look like this when we convert it to code:
      ```
      i {
        e s {x ib3x ib2x ib1x sb1n sb2n sb3n n a}
      }
      s {
        e i {}
      }
      ```

### Diagramming languages
| 	                                                    | **Mermaid**                                                               	                                             | **PlantUML**                                                 	 |
|------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------|
| **Entry- and Exit-Actions** <br>(aka, description) 	 | ❌ (We can use `\n` to write description line)                                                                         	 | [✅ See](https://plantuml.com/state-diagram#7d9e703ac421ea25) 	 |
| **Super State**<br>(aka, Composite State)          	 | [✅ See](https://mermaid.js.org/syntax/stateDiagram.html#composite-states) 	                                             | [✅ See](https://plantuml.com/state-diagram#a70cc614da79064a) 	 |

- **Entry- and Exit-Actions** (aka, description)  
  ![Entry- and Exit-Actions](imgs/entry-exit-actions.png)
- **Super State** (aka, Composite State)      
  ![Super State](imgs/super-state.png)

### Optimized and non-optimized state diagram
#### Optimized state diagram
  A state diagram generated form optimized
  code, and it doesn't have any human-intuitive/syntactic sugar diagramming
  elements like entry- and exit-actions and super/composite state. It 
  **only** consists of state, event, next state and actions.
#### Non-optimized state diagram
  A state diagram generated form
  non-optimized code, and it could consist of human-intuitive/syntactic 
  sugar diagramming elements like entry- and exit-actions and super/composite 
  state along with basic elements like state, event, next state and 
  actions.

## Context and History
The original cc_smc source code is allowing you to add new language by:   
1. Create `XXXNode` and `XXXNodeVisitor`.
2. Create `XXXGenerator`.
3. Create `YYYXXXImplementer` which implements `XXXNodeVisitor`.
4. Create `YYYCodeGenerator` which extends `CodeGenerator`, and use
   `XXXGenerator`, `YYYXXXImplementer` and `OptimizedStateMachine` of 
   the code to generate a tree of `XXXNode`s then the output code.

### What we have today
1. `CodeGenerator` class is depending on `NSCGenerator`, so any 
   new programming language, which will subclass `CodeGenerator`, is a 
   must to be generated as Nested Switch Case code.  
   ```java
   public abstract class CodeGenerator {
   //...
      public void generate() throws IOException {
         NSCGenerator nscGenerator = new NSCGenerator();
         nscGenerator.generate(optimizedStateMachine).accept(getImplementer());
         writeFiles();
      }
   //...
   }
   ```
   
2. `CodeGenerator` and `YYYCodeGenerator` classes are depending on 
   `OptimizedStateMachine`, you cannot generate code directly from 
   `SemanticStateMachine`. Generating code directly from 
   `SemanticStateMachine` without optimizing is useful in diagramming 
   as it's more human-intuitive.

## Goals
### Code-style-agnostic `CodeGenerator`
Make `CodeGenerator` base class not depending on specific `XXXGenerator` 
(e.g., `NSCGenerator`) but each child from it determines what 
`XXXGenerator` class to use, so can generate code other than Nested 
Switch Case.

### Optimization-agnostic `CodeGenerator`
Generate optimized and non-optimized output code, as user determine in 
CLI flag `optimized:true` or `optimized:false`. 
We can achieve this goal either by:  
1. Using same `DiagramGenerator` to build a tree of `DiagramNode`s
   for both optimized and non-optimized state machine DSs. This will lead 
   to using same interface for both `OptimizedStateMachine` and 
   `SemanticStateMachine` (e.g., creating a base class/interface for both)
   and maybe same components (e.g., `Header`, `Transition`, ...).
2. Creating a `OptimizedDiagramGenerator` for optimized code generation
   process and `NonOptimizedDiagramGenerator` for non-optimized code 
   generation process. Both will build a tree of `DiagramNode`s that can 
   be used later with the same `PlantUMLDiagramImplementer`.  

These two alternatives will be discussed in detail, in survey section. 

### Mermaid code generation
PlantUML is perfect to our case, it supports both entry- and exit-actions
and composite state, so we can generate both optimized and 
non-optimized diagrams but what if we want to generate mermaid 
code?  
We can achieve this goal either by:
1. Using `\n` in the state description but this will be ugly.
2. Generating only optimized diagrams, so no need for supporting 
  entry- and exit-actions feature.
#### Shortlisting
I will choose the 2nd solution because we will already implement 
a non-optimized diagram generation with PlantUML, no need for ugly
unused feature, it's low priority and can be implemented by the 
community in the future.

## Survey
In this survey, I'll discuss "Goals > Optimization-agnostic 
`CodeGenerator`" in detail, the other two goals will be designed
directly in the "Design" section because they are pretty easy.

### 1. `DiagramGenerator`
![1st Method](imgs/method1.png)
#### Pros
- Less code, same source code for optimized and non-optimized diagrams
  generation.
- Preserve abstraction.
- Use same implementer for both.
#### Cons
- Non-optimized code for NSC-style, as an example, cannot be 
  generated because there is no concepts like composite state in this
  style, so this will violate LSP or SRP, depending on the implementation.
- While it appears as it preserves Open-Closed Principle but the fact
  is that there are and will be **only** two cases either optimized or 
  non-optimized state machine DSs, so no need for this extra polymorphic
  behaviour.
- It's hard to generate state machine DSs that has the same interface
  and same component interfaces because for example state in 
  non-optimized state machine DS has more properties than optimized 
  one and this may violate LSP.
  
### 2. `OptimizedDiagramGenerator` and `NonOptimizedDiagramGenerator`
![2nd Method](imgs/method2.png)
#### Pros
- Maintain SRP.
- Its behaviour is polymorphic for some extent.
- Use same implementer for both.
#### Cons
- Write a code x2 but this may consider as a pro due to avoiding 
  handling edge cases for both state machine DSs, if we couldn't
  reach a proper abstraction, this may result in SRP violation.

**Errata**: 
I mistakenly typed `PlantUMLCodeGenerator` and `MermaidCodeGenerator`
in the previous images, however it should be `PlantUMLDiagramGenerator`
and `MermaidDiagramGenerator`.

## Design
### Code-style-agnostic `CodeGenerator`
This is the current diagram:
![Problem1](imgs/p1.png)
And this is the proposed solution:
![Sol1](imgs/sol1.png)
### Optimization-agnostic `CodeGenerator`
This is the proposed solution:
![Sol2](imgs/method2.png)

## Implementation Plan
**Phase 1**: Make `CodeGenerator` more abstract (i.e., implement
code-style-agnostic feature).    
**Phase 2**: **Design** and implement `DiagramNode` and 
`DiagramNodeVisitor` according to PlantUML and Mermaid syntax.   
**Phase 3**: Create `PlantUMLDiagramGenerator`, 
`MermaidDiagramGenerator`, `OptimizedDiagramGenerator`, 
`NonOptimizedDiagramGenerator`, `PlantUMLDiagramImplementer` and
`MermaidDiagramImplementer`. (These can be created in parallel)
