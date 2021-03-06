import org.apache.shiro.crypto.hash.Sha256Hash
import org.apache.shiro.SecurityUtils
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import org.springframework.context.ApplicationContext
import grails.util.Environment
import shop.stock.Product
import shop.stock.ProductImage
import cms.Role
import cms.Profile
import cms.LocalLogon
import cms.Logon

class BootStrap {

  def wcmContentRepositoryService

  def init = { servletContext ->
      ApplicationContext context = servletContext.getAttribute(GrailsApplicationAttributes.APPLICATION_CONTEXT)

      maybeSetupSecurityInfo()

      if (Environment.current == Environment.DEVELOPMENT) {
        setupDevProducts()
      }

      context.wcmSecurityService.securityDelegate = [
        getUserName : { ->
          if (SecurityUtils.subject?.principal) {
            return Logon.findByIdentifier(SecurityUtils.subject.principal)?.profile?.displayName
          }
          return ""
        },
        getUserEmail : { ->
          if (SecurityUtils.subject?.principal) {
            return Logon.findByIdentifier(SecurityUtils.subject.principal)?.profile?.email
          }
          return ""
        },
        getUserRoles : { ->
          return ["ROLE_ADMIN"]
        },
        getUserPrincipal : { ->
          return SecurityUtils.subject.principal ?: [:]
        }
      ]
    }

    def setupDevProducts() {
        if (!Product.findByProductCode("12345")) {
          Product prod = new Product(name: "Testing Product", productCode: "12345", category: "ring")
          if (!prod.save()) {
            prod.errors.allErrors.each {
              println it
            }
          }

          prod.addToSkus(stockCode:"12345-stock",
                        price:45.99,
                        costPrice:13.99,
                        inventoryLevel:2)

          prod.addToSkus(stockCode:"12345-stock2",
                        price:47.99,
                        costPrice:13.99,
                        inventoryLevel:4)

          prod = new Product(name: "Wibble hello", productCode: "45679", category: "necklace")

          prod.addToSkus(stockCode:"igglestock",
                        price:12.99,
                        costPrice:13.99,
                        inventoryLevel:0)

          prod.addToSkus(stockCode:"igglestock13",
                        price:14.99,
                        costPrice:13.99,
                        inventoryLevel:99)

          if (!prod.save()) {
            prod.errors.allErrors.each {
              println it
            }
          }
          new ProductImage(product: prod, location: "/images/grails_logo.jpg").save()
          new ProductImage(product: prod, location: "/images/spinner.gif").save()
      }
    }

    def maybeSetupSecurityInfo() {

      if (!Role.findByName("editor")) {
        Role userRole = new Role(name:"editor")
        userRole.addToPermissions("profile:showCurrent")
        userRole.addToPermissions("wcmRepository:*")
        userRole.addToPermissions("wcmPortal:*")
        userRole.addToPermissions("wcmEditor:*")
        userRole.addToPermissions("wcmArchive:*")
        userRole.addToPermissions("wcmContent:*")
        userRole.addToPermissions("wcmContentSubmission:*")
        userRole.addToPermissions("wcmPortal:*")
        userRole.addToPermissions("wcmSearch:*")
        userRole.addToPermissions("wcmSynchronization:*")
        userRole.addToPermissions("wcmWikiItemRender:*")
        userRole.save()

        //make a profile
        Profile profile = new Profile(displayName:"Some User", email:"someone@c.com")
        profile.addToRoles(userRole)
        profile.addToPermittedSpaces(wcmContentRepositoryService.findDefaultSpace())
        profile = profile.save(flush:true, failOnError:true)

        new LocalLogon(identifier:profile.email, passwordHash: new Sha256Hash("examplepass").toHex(), profile: profile).save(flush:true, failOnError:true)

      }

      if (!Role.findByName("admin")) {
        log.info("Generating Admin Role")
        Role userRole = new Role(name:"admin")
        userRole.addToPermissions("*:*")
        userRole.save()

        //make a profile
        Profile profile = new Profile(displayName:"David Dawson", email:"david.dawson@dawsonsystems.com")
        profile.addToRoles(userRole)
        profile = profile.save(flush:true, failOnError:true)

        new LocalLogon(identifier:profile.email, passwordHash: new Sha256Hash("examplepass").toHex(), profile: profile).save(flush:true, failOnError:true)
      }
    }

    def destroy = {
    }
}
