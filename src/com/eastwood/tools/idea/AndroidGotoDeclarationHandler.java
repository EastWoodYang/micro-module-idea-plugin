package com.eastwood.tools.idea;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlElement;
import org.jetbrains.android.dom.AndroidAttributeValue;
import org.jetbrains.android.dom.manifest.Manifest;
import org.jetbrains.android.dom.manifest.ManifestElementWithRequiredName;
import org.jetbrains.android.dom.resources.Attr;
import org.jetbrains.android.dom.resources.DeclareStyleable;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.android.resourceManagers.LocalResourceManager;
import org.jetbrains.android.resourceManagers.ModuleResourceManagers;
import org.jetbrains.android.resourceManagers.ResourceManager;
import org.jetbrains.android.util.AndroidResourceUtil;
import org.jetbrains.android.util.AndroidUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.jetbrains.android.util.AndroidResourceUtil.isManifestJavaFile;
import static org.jetbrains.android.util.AndroidResourceUtil.isRJavaFile;

public class AndroidGotoDeclarationHandler extends org.jetbrains.android.AndroidGotoDeclarationHandler {

    @Override
    public PsiElement[] getGotoDeclarationTargets(@Nullable PsiElement sourceElement, int offset, Editor editor) {
        if (!(sourceElement instanceof PsiIdentifier)) {
            return null;
        } else {
            PsiFile file = sourceElement.getContainingFile();
            if (file == null) {
                return null;
            } else {
                AndroidFacet facet = AndroidFacet.getInstance(file);
                if (facet == null) {
                    return null;
                } else {
                    PsiReferenceExpression refExp = (PsiReferenceExpression) PsiTreeUtil.getParentOfType(sourceElement, PsiReferenceExpression.class);
                    if (refExp == null) {
                        return null;
                    } else {
                        AndroidResourceUtil.MyReferredResourceFieldInfo info = getReferredResourceOrManifestField(facet, refExp, false);
                        if (info == null) {
                            PsiElement parent = refExp.getParent();
                            if (parent instanceof PsiReferenceExpression) {
                                info = getReferredResourceOrManifestField(facet, (PsiReferenceExpression)parent, false);
                            }

                            if (info == null) {
                                parent = parent.getParent();
                                if (parent instanceof PsiReferenceExpression) {
                                    info = getReferredResourceOrManifestField(facet, (PsiReferenceExpression)parent, false);
                                }
                            }
                        }

                        if (info == null) {
                            return null;
                        } else {
                            String nestedClassName = info.getClassName();
                            String fieldName = info.getFieldName();
                            List<PsiElement> resourceList = new ArrayList();
                            if (info.isFromManifest()) {
                                collectManifestElements(nestedClassName, fieldName, facet, resourceList);
                            } else {
                                ModuleResourceManagers resourceManagers = ModuleResourceManagers.getInstance(facet);
                                ResourceManager manager = info.isSystem() ? resourceManagers.getSystemResourceManager(false) : resourceManagers.getLocalResourceManager();
                                if (manager == null) {
                                    return null;
                                }

                                ((ResourceManager)manager).collectLazyResourceElements(nestedClassName, fieldName, false, refExp, resourceList);
                                if (manager instanceof LocalResourceManager) {
                                    LocalResourceManager lrm = (LocalResourceManager)manager;
                                    Iterator var14;
                                    Attr styleable;
                                    if (nestedClassName.equals("attr")) {
                                        var14 = lrm.findAttrs(fieldName).iterator();

                                        while(var14.hasNext()) {
                                            styleable = (Attr)var14.next();
                                            resourceList.add(styleable.getName().getXmlAttributeValue());
                                        }
                                    } else if (nestedClassName.equals("styleable")) {
                                        var14 = lrm.findStyleables(fieldName).iterator();

                                        while(var14.hasNext()) {
                                            DeclareStyleable declareStyleable = (DeclareStyleable)var14.next();
                                            resourceList.add(declareStyleable.getName().getXmlAttributeValue());
                                        }

                                        var14 = lrm.findStyleableAttributesByFieldName(fieldName).iterator();

                                        while(var14.hasNext()) {
                                            styleable = (Attr)var14.next();
                                            resourceList.add(styleable.getName().getXmlAttributeValue());
                                        }
                                    }
                                }
                            }

                            if (resourceList.size() > 1) {
                                resourceList.sort(AndroidResourceUtil.RESOURCE_ELEMENT_COMPARATOR);
                            }

                            return (PsiElement[])resourceList.toArray(new PsiElement[resourceList.size()]);
                        }
                    }
                }
            }
        }
    }

    private static void collectManifestElements(@NotNull String nestedClassName, @NotNull String fieldName, @NotNull AndroidFacet facet, @NotNull List<PsiElement> result) {
        Manifest manifest = facet.getManifest();
        if (manifest != null) {
            List list;
            if ("permission".equals(nestedClassName)) {
                list = manifest.getPermissions();
            } else {
                if (!"permission_group".equals(nestedClassName)) {
                    return;
                }

                list = manifest.getPermissionGroups();
            }

            Iterator var6 = list.iterator();

            while(var6.hasNext()) {
                ManifestElementWithRequiredName domElement = (ManifestElementWithRequiredName)var6.next();
                AndroidAttributeValue<String> nameAttribute = domElement.getName();
                String name = (String)nameAttribute.getValue();
                if (AndroidUtils.equal(name, fieldName, false)) {
                    XmlElement psiElement = nameAttribute.getXmlAttributeValue();
                    if (psiElement != null) {
                        result.add(psiElement);
                    }
                }
            }

        }
    }

    @Nullable
    public static AndroidResourceUtil.MyReferredResourceFieldInfo getReferredResourceOrManifestField(@NotNull AndroidFacet facet, @NotNull PsiReferenceExpression exp, boolean localOnly) {
        return getReferredResourceOrManifestField(facet, exp, (String)null, localOnly);
    }

    @Nullable
    public static AndroidResourceUtil.MyReferredResourceFieldInfo getReferredResourceOrManifestField(@NotNull AndroidFacet facet, @NotNull PsiReferenceExpression exp, @Nullable String className, boolean localOnly) {
        String resFieldName = exp.getReferenceName();
        if (resFieldName != null && !resFieldName.isEmpty()) {
            PsiExpression qExp = exp.getQualifierExpression();
            if (!(qExp instanceof PsiReferenceExpression)) {
                return null;
            } else {
                PsiReferenceExpression resClassReference = (PsiReferenceExpression)qExp;
                String resClassName = resClassReference.getReferenceName();
                if (resClassName == null || resClassName.isEmpty() || className != null && !className.equals(resClassName)) {
                    return null;
                } else {
                    qExp = resClassReference.getQualifierExpression();
                    if (!(qExp instanceof PsiReferenceExpression)) {
                        return null;
                    } else {
                        PsiElement resolvedElement = ((PsiReferenceExpression)qExp).resolve();
                        if (!(resolvedElement instanceof PsiClass)) {
                            return null;
                        } else {
                            Module resolvedModule = ModuleUtilCore.findModuleForPsiElement(resolvedElement);
                            PsiClass aClass = (PsiClass)resolvedElement;
                            String classShortName = aClass.getName();
                            boolean fromManifest = "Manifest".equals(classShortName);
                            if (!fromManifest && !"R".equals(classShortName)) {
                                return null;
                            } else {
                                if (!localOnly) {
                                    String qName = aClass.getQualifiedName();
                                    if ("android.R".equals(qName) || "com.android.internal.R".equals(qName)) {
                                        return new AndroidResourceUtil.MyReferredResourceFieldInfo(resClassName, resFieldName, resolvedModule, true, false);
                                    }
                                }

                                PsiFile containingFile = resolvedElement.getContainingFile();
                                if (containingFile == null) {
                                    return null;
                                } else {
                                    if (fromManifest) {
                                        if (!isManifestJavaFile(facet, containingFile)) {
                                            return null;
                                        }
                                    } else if (!isRJavaFile(facet, containingFile)) {
                                        if(!isMicroModuleRJavaFile(containingFile)) {
                                            return null;
                                        }
                                    }

                                    return new AndroidResourceUtil.MyReferredResourceFieldInfo(resClassName, resFieldName, resolvedModule, false, fromManifest);
                                }
                            }
                        }
                    }
                }
            }
        } else {
            return null;
        }
    }

    private static boolean isMicroModuleRJavaFile(@NotNull PsiFile file) {
        if (file.getName().equals("R.java") && file instanceof PsiJavaFile) {
            if(file.getVirtualFile().getPath().contains("build")) {
                return true;
            }
        }
        return false;
    }

}
